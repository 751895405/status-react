(ns status-im.network.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.accounts.update.core :as accounts.update]
            [status-im.i18n :as i18n]
            [status-im.native-module.core :as status]
            [status-im.network.net-info :as net-info]
            [status-im.transport.inbox :as inbox]
            [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.fx :as fx]))

(def url-regex
  #"https?://(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}(\.[a-z]{2,6})?\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)")

(defn valid-rpc-url? [url]
  (boolean (re-matches url-regex (str url))))

(def default-manage
  {:name  {:value ""}
   :url   {:value ""}
   :chain {:value :mainnet}})

(defn validate-string [{:keys [value]}]
  {:value value
   :error (string/blank? value)})

(defn validate-url [{:keys [value]}]
  {:value value
   :error (not (valid-rpc-url? value))})

(defn validate-manage [manage]
  (-> manage
      (update :url validate-url)
      (update :name validate-string)
      (update :chain validate-string)))

(defn valid-manage? [manage]
  (->> (validate-manage manage)
       vals
       (map :error)
       (not-any? identity)))

(defn new-network [random-id network-name upstream-url type network-id]
  (let [data-dir (str "/ethereum/" (name type) "_rpc")
        config   {:NetworkId      (or (when network-id (int network-id))
                                      (ethereum/chain-keyword->chain-id type))
                  :DataDir        data-dir
                  :UpstreamConfig {:Enabled true
                                   :URL     upstream-url}}]
    {:id         (string/replace random-id "-" "")
     :name       network-name
     :config     config}))

(defn get-chain [{:keys [db]}]
  (let [network  (get (:networks (:account/account db)) (:network db))]
    (ethereum/network->chain-keyword network)))

(fx/defn set-input
  [{:keys [db]} input-key value]
  {:db (-> db
           (update-in [:networks/manage input-key] assoc :value value)
           (update-in [:networks/manage] validate-manage))})

(defn- action-handler
  ([handler]
   (action-handler handler nil nil))
  ([handler data cofx]
   (when handler
     (handler data cofx))))

(defn save
  ([cofx]
   (save cofx nil))
  ([{{:network/keys [manage]
      :account/keys [account] :as db} :db :as cofx}
    {:keys [data success-event on-success on-failure]}]
   (let [data (or data manage)]
     (if (valid-manage? data)
       (let [{:keys [name url chain network-id]} data
             network      (new-network (:random-id cofx)
                                       (:value name)
                                       (:value url)
                                       (:value chain)
                                       (:value network-id))
             new-networks (merge {(:id network) network} (:networks account))]
         (fx/merge cofx
                   {:db (dissoc db :networks/manage)}
                   #(action-handler on-success (:id network) %)
                   (accounts.update/account-update
                    {:networks new-networks}
                    {:success-event success-event})))
       (action-handler on-failure)))))

;; No edit functionality actually implemented
(fx/defn edit
  [{db :db}]
  {:db       (assoc db :networks/manage (validate-manage default-manage))
   :dispatch [:navigate-to :edit-network]})

(fx/defn connect [{:keys [db now] :as cofx} {:keys [network on-success on-failure]}]
  (if (get-in db [:account/account :networks network])
    (let [current-network (get-in db [:account/account :networks (:network db)])]
      (if (ethereum/network-with-upstream-rpc? current-network)
        (fx/merge cofx
                  #(action-handler on-success network %)
                  (accounts.update/account-update
                   {:network      network
                    :last-updated now}
                   {:success-event [:accounts.update.callback/save-settings-success]}))
        (fx/merge cofx
                  {:ui/show-confirmation {:title               (i18n/label :t/close-app-title)
                                          :content             (i18n/label :t/close-app-content)
                                          :confirm-button-text (i18n/label :t/close-app-button)
                                          :on-accept           #(re-frame/dispatch [:network.ui/save-non-rpc-network-pressed network])
                                          :on-cancel           nil}}
                  #(action-handler on-success network %))))
    (action-handler on-failure)))

(fx/defn delete
  [{{:account/keys [account]} :db :as cofx} {:keys [network on-success on-failure]}]
  (let [current-network? (= (:network account) network)]
    (if (or current-network?
            (not (get-in account [:networks network])))
      (fx/merge cofx
                {:ui/show-error (i18n/label :t/delete-network-error)}
                #(action-handler on-failure network %))
      (fx/merge cofx
                {:ui/show-confirmation {:title               (i18n/label :t/delete-network-title)
                                        :content             (i18n/label :t/delete-network-confirmation)
                                        :confirm-button-text (i18n/label :t/delete)
                                        :on-accept           #(re-frame/dispatch [:network.ui/remove-network-confirmed network])
                                        :on-cancel           nil}}
                #(action-handler on-success network %)))))

(fx/defn save-non-rpc-network
  [{:keys [db now] :as cofx} network]
  (accounts.update/account-update cofx
                                  {:network      network
                                   :last-updated now}
                                  {:success-event [:network.callback/non-rpc-network-saved]}))

(fx/defn remove-network
  [{:keys [db now] :as cofx} network]
  (let [networks (dissoc (get-in db [:account/account :networks]) network)]
    (accounts.update/account-update cofx
                                    {:networks     networks
                                     :last-updated now}
                                    {:success-event [:navigate-back]})))

(fx/defn save-network
  [cofx]
  (save cofx
        {:data          (get-in cofx [:db :networks/manage])
         :success-event [:navigate-back]}))

(fx/defn handle-connection-status-change
  [{:keys [db] :as cofx} is-connected?]
  (fx/merge cofx
            {:db (assoc db :network-status (if is-connected? :online :offline))}
            (inbox/request-messages)))

(fx/defn handle-network-status-change
  [cofx data]
  {:network/notify-status-go data})

(re-frame/reg-fx
 :network/listen-to-network-status
 (fn []
   (let [callback-event #(re-frame/dispatch [:network/network-status-changed %])]
     (net-info/net-info callback-event)
     (net-info/add-net-info-listener callback-event))))

(re-frame/reg-fx
 :network/listen-to-connection-status
 (fn []
   (let [callback-event #(re-frame/dispatch [:network/connection-status-changed %])]
     (net-info/is-connected? callback-event)
     (net-info/add-connection-listener callback-event))))

(re-frame/reg-fx
 :network/notify-status-go
 (fn [data]
   (status/connection-change data)))
