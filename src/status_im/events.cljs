(ns status-im.events
  (:require [re-frame.core :as re-frame]
            [status-im.accounts.core :as accounts]
            [status-im.accounts.create.core :as accounts.create]
            [status-im.accounts.login.core :as accounts.login]
            [status-im.accounts.logout.core :as accounts.logout]
            [status-im.accounts.recover.core :as accounts.recover]
            [status-im.accounts.update.core :as accounts.update]
            [status-im.bootnodes.core :as bootnodes]
            [status-im.browser.core :as browser]
            [status-im.browser.permissions :as browser.permissions]
            [status-im.data-store.core :as data-store]
            [status-im.fleet.core :as fleet]
            [status-im.hardwallet.core :as hardwallet]
            [status-im.i18n :as i18n]
            [status-im.init.core :as init]
            [status-im.log-level.core :as log-level]
            [status-im.mailserver.core :as mailserver]
            [status-im.network.core :as network]
            [status-im.notifications.core :as notifications]
            [status-im.privacy-policy.core :as privacy-policy]
            [status-im.protocol.core :as protocol]
            [status-im.qr-scanner.core :as qr-scanner]
            [status-im.signals.core :as signals]
            [status-im.ui.screens.currency-settings.models
             :as
             currency-settings.models]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [status-im.utils.handlers :as handlers]
            [taoensso.timbre :as log]))

;; init module

(handlers/register-handler-fx
 :init.ui/data-reset-accepted
 (fn [cofx _]
   {:init/reset-data nil}))

(handlers/register-handler-fx
 :init.ui/data-reset-cancelled
 (fn [cofx [_ encryption-key]]
   (init/initialize-app cofx encryption-key)))

(handlers/register-handler-fx
 :init/app-started
 (fn [cofx _]
   (init/initialize-keychain cofx)))

(handlers/register-handler-fx
 :init.callback/get-encryption-key-success
 (fn [cofx [_ encryption-key]]
   (init/initialize-app cofx encryption-key)))

(handlers/register-handler-fx
 :init.callback/get-device-UUID-success
 (fn [cofx [_ device-uuid]]
   (init/set-device-uuid cofx device-uuid)))

(handlers/register-handler-fx
 :init.callback/init-store-success
 [(re-frame/inject-cofx :data-store/get-all-accounts)]
 (fn [cofx _]
   (init/load-accounts-and-initialize-views cofx)))

(handlers/register-handler-fx
 :init.callback/init-store-error
 (fn [cofx [_ encryption-key error]]
   (init/handle-init-store-error cofx encryption-key)))

(handlers/register-handler-fx
 :init.callback/account-change-success
 [(re-frame/inject-cofx :web3/get-web3)
  (re-frame/inject-cofx :get-default-contacts)
  (re-frame/inject-cofx :get-default-dapps)
  (re-frame/inject-cofx :data-store/all-chats)
  (re-frame/inject-cofx :data-store/get-messages)
  (re-frame/inject-cofx :data-store/get-user-statuses)
  (re-frame/inject-cofx :data-store/get-unviewed-messages)
  (re-frame/inject-cofx :data-store/message-ids)
  (re-frame/inject-cofx :data-store/get-local-storage-data)
  (re-frame/inject-cofx :data-store/get-all-contacts)
  (re-frame/inject-cofx :data-store/get-all-mailservers)
  (re-frame/inject-cofx :data-store/transport)
  (re-frame/inject-cofx :data-store/all-browsers)
  (re-frame/inject-cofx :data-store/all-dapp-permissions)]
 (fn [cofx [_ address]]
   (init/initialize-account cofx address)))

(handlers/register-handler-fx
 :init.callback/account-change-error
 (fn [cofx _]
   (init/handle-change-account-error cofx)))

(handlers/register-handler-fx
 :init.callback/keychain-reset
 (fn [cofx _]
   (init/initialize-keychain cofx)))

;; accounts module

(handlers/register-handler-fx
 :accounts.ui/mainnet-warning-shown
 (fn [cofx _]
   (accounts.update/account-update cofx {:mainnet-warning-shown? true} {})))

(handlers/register-handler-fx
 :accounts.ui/dev-mode-switched
 (fn [cofx [_ dev-mode?]]
   (accounts/switch-dev-mode cofx dev-mode?)))

(handlers/register-handler-fx
 :accounts.ui/web3-opt-in-mode-switched
 (fn [cofx [_ opt-in]]
   (accounts/switch-web3-opt-in-mode cofx opt-in)))

(handlers/register-handler-fx
 :accounts.ui/wallet-set-up-confirmed
 (fn [cofx [_ modal?]]
   (accounts/confirm-wallet-set-up cofx modal?)))

;; accounts create module

(handlers/register-handler-fx
 :accounts.create.ui/next-step-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts.create/next-step cofx step password password-confirm)))

(handlers/register-handler-fx
 :accounts.create.ui/step-back-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts.create/step-back cofx step)))

(handlers/register-handler-fx
 :accounts.create.ui/input-text-changed
 (fn [cofx [_ input-key text]]
   (accounts.create/account-set-input-text cofx input-key text)))

(handlers/register-handler-fx
 :accounts.create.callback/create-account-success
 [(re-frame/inject-cofx :random-guid-generator)
  (re-frame/inject-cofx :accounts.create/get-signing-phrase)
  (re-frame/inject-cofx :accounts.create/get-status)]
 (fn [cofx [_ result password]]
   (accounts.create/on-account-created cofx result password false)))

(handlers/register-handler-fx
 :accounts.create.ui/create-new-account-button-pressed
 (fn [cofx _]
   (accounts.create/navigate-to-authentication-method cofx)))

;; accounts recover module

(handlers/register-handler-fx
 :accounts.recover.ui/recover-account-button-pressed
 (fn [cofx _]
   (accounts.recover/navigate-to-recover-account-screen cofx)))

(handlers/register-handler-fx
 :accounts.recover.ui/passphrase-input-changed
 (fn [cofx [_ recovery-phrase]]
   (accounts.recover/set-phrase cofx recovery-phrase)))

(handlers/register-handler-fx
 :accounts.recover.ui/passphrase-input-blured
 (fn [cofx _]
   (accounts.recover/validate-phrase cofx)))

(handlers/register-handler-fx
 :accounts.recover.ui/password-input-changed
 (fn [cofx [_ masked-password]]
   (accounts.recover/set-password cofx masked-password)))

(handlers/register-handler-fx
 :accounts.recover.ui/password-input-blured
 (fn [cofx _]
   (accounts.recover/validate-password cofx)))

(handlers/register-handler-fx
 :accounts.recover.ui/sign-in-button-pressed
 (fn [cofx _]
   (accounts.recover/recover-account-with-checks cofx)))

(handlers/register-handler-fx
 :accounts.recover.ui/recover-account-confirmed
 (fn [cofx _]
   (accounts.recover/recover-account cofx)))

(handlers/register-handler-fx
 :accounts.recover.callback/recover-account-success
 [(re-frame/inject-cofx :random-guid-generator)
  (re-frame/inject-cofx :accounts.create/get-signing-phrase)
  (re-frame/inject-cofx :accounts.create/get-status)]
 (fn [cofx [_ result password]]
   (accounts.recover/on-account-recovered cofx result password)))

;; accounts login module

(handlers/register-handler-fx
 :accounts.login.ui/password-input-submitted
 (fn [cofx _]
   (accounts.login/user-login cofx)))

(handlers/register-handler-fx
 :accounts.login.callback/login-success
 (fn [cofx [_ login-result]]
   (accounts.login/user-login-callback cofx login-result)))

(handlers/register-handler-fx
 :accounts.login.ui/account-selected
 (fn [cofx [_ address photo-path name]]
   (accounts.login/open-login cofx address photo-path name)))

(handlers/register-handler-fx
 :accounts.login.callback/get-user-password-success
 (fn [cofx [_ password]]
   (accounts.login/open-login-callback cofx password)))

;; accounts logout module

(handlers/register-handler-fx
 :accounts.logout.ui/logout-pressed
 (fn [cofx _]
   (accounts.logout/show-logout-confirmation cofx)))

(handlers/register-handler-fx
 :accounts.logout.ui/logout-confirmed
 (fn [cofx _]
   (accounts.logout/logout cofx)))

;; accounts update module

(handlers/register-handler-fx
 :accounts.update.callback/save-settings-success
 (fn [cofx _]
   (accounts.logout/logout cofx)))

;; mailserver module

(handlers/register-handler-fx
 :mailserver.ui/user-defined-mailserver-selected
 (fn [cofx [_ mailserver-id]]
   (mailserver/edit cofx mailserver-id)))

(handlers/register-handler-fx
 :mailserver.ui/default-mailserver-selected
 (fn [cofx [_ mailserver-id]]
   (mailserver/show-connection-confirmation cofx mailserver-id)))

(handlers/register-handler-fx
 :mailserver.ui/add-pressed
 (fn [cofx _]
   (navigation/navigate-to-cofx cofx :edit-mailserver nil)))

(handlers/register-handler-fx
 :mailserver.ui/save-pressed
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx _]
   (mailserver/upsert cofx)))

(handlers/register-handler-fx
 :mailserver.ui/input-changed
 (fn [cofx [_ input-key value]]
   (mailserver/set-input cofx input-key value)))

(handlers/register-handler-fx
 :mailserver.ui/delete-confirmed
 (fn [cofx [_ mailserver-id]]
   (mailserver/delete mailserver-id cofx)))

(handlers/register-handler-fx
 :mailserver.ui/delete-pressed
 (fn [cofx [_ mailserver-id]]
   (mailserver/show-delete-confirmation cofx mailserver-id)))

(handlers/register-handler-fx
 :mailserver.callback/qr-code-scanned
 (fn [cofx [_ _ url]]
   (mailserver/set-url-from-qr cofx url)))

(handlers/register-handler-fx
 :mailserver.ui/connect-pressed
 (fn [cofx [_  mailserver-id]]
   (mailserver/show-connection-confirmation cofx mailserver-id)))

(handlers/register-handler-fx
 :mailserver.ui/connect-confirmed
 (fn [cofx [_ current-fleet mailserver-id]]
   (mailserver/save-settings cofx current-fleet mailserver-id)))

;; network module

(handlers/register-handler-fx
 :network.ui/save-network-pressed
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx]
   (network/save-network cofx)))

(handlers/register-handler-fx
 :network.ui/input-changed
 (fn [cofx [_ input-key value]]
   (network/set-input cofx input-key value)))

(handlers/register-handler-fx
 :network.ui/add-network-pressed
 (fn [cofx]
   (network/edit cofx)))

(handlers/register-handler-fx
 :network.callback/non-rpc-network-saved
 (fn [_ _]
   {:ui/close-application nil}))

(handlers/register-handler-fx
 :network.ui/save-non-rpc-network-pressed
 (fn [cofx [_ network]]
   (network/save-non-rpc-network cofx network)))

(handlers/register-handler-fx
 :network.ui/remove-network-confirmed
 (fn [cofx [_ network]]
   (network/remove-network cofx network)))

(handlers/register-handler-fx
 :network.ui/connect-network-pressed
 (fn [cofx [_ network]]
   (network/connect cofx {:network network})))

(handlers/register-handler-fx
 :network.ui/delete-network-pressed
 (fn [cofx [_ network]]
   (network/delete cofx {:network network})))

(handlers/register-handler-fx
 :network/connection-status-changed
 (fn [{db :db :as cofx} [_ is-connected?]]
   (network/handle-connection-status-change cofx is-connected?)))

(handlers/register-handler-fx
 :network/network-status-changed
 (fn [cofx [_ data]]
   (network/handle-network-status-change cofx data)))

;; fleet module

(handlers/register-handler-fx
 :fleet.ui/save-fleet-confirmed
 (fn [cofx [_ fleet]]
   (fleet/save cofx fleet)))

(handlers/register-handler-fx
 :fleet.ui/fleet-selected
 (fn [cofx [_ fleet]]
   (fleet/show-save-confirmation cofx fleet)))

;; bootnodes module

(handlers/register-handler-fx
 :bootnodes.ui/custom-bootnodes-switch-toggled
 (fn [cofx [_ value]]
   (bootnodes/toggle-custom-bootnodes cofx value)))

(handlers/register-handler-fx
 :bootnodes.ui/add-bootnode-pressed
 (fn [cofx [_ bootnode-id]]
   (bootnodes/edit cofx bootnode-id)))

(handlers/register-handler-fx
 :bootnodes.callback/qr-code-scanned
 (fn [cofx [_ _ url]]
   (bootnodes/set-bootnodes-from-qr cofx url)))

(handlers/register-handler-fx
 :bootnodes.ui/input-changed
 (fn [cofx [_ input-key value]]
   (bootnodes/set-input cofx input-key value)))

(handlers/register-handler-fx
 :bootnodes.ui/save-pressed
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx _]
   (bootnodes/upsert cofx)))

(handlers/register-handler-fx
 :bootnodes.ui/delete-pressed
 (fn [_ [_ id]]
   (bootnodes/show-delete-bootnode-confirmation _ id)))

(handlers/register-handler-fx
 :bootnodes.ui/delete-confirmed
 (fn [cofx [_ bootnode-id]]
   (bootnodes/delete-bootnode cofx bootnode-id)))

;; log-level module

(handlers/register-handler-fx
 :log-level.ui/change-log-level-confirmed
 (fn [cofx [_ log-level]]
   (log-level/save-log-level cofx log-level)))

(handlers/register-handler-fx
 :log-level.ui/log-level-selected
 (fn [cofx [_ log-level]]
   (log-level/show-change-log-level-confirmation cofx log-level)))

;; Browser bridge module

(handlers/register-handler-fx
 :browser.bridge.callback/qr-code-scanned
 (fn [cofx [_ _ data message]]
   (browser/handle-scanned-qr-code cofx data message)))

;; qr-scanner module

(handlers/register-handler-fx
 :qr-scanner.ui/scan-qr-code-pressed
 (fn [cofx [_ identifier handler & [opts]]]
   (qr-scanner/scan-qr-code cofx identifier (merge {:handler handler} opts))))

(handlers/register-handler-fx
 :qr-scanner.callback/scan-qr-code-success
 (fn [cofx [_ context data]]
   (qr-scanner/set-qr-code cofx context data)))

;; privacy-policy module

(handlers/register-handler-fx
 :privacy-policy/privacy-policy-button-pressed
 (fn [cofx _]
   (privacy-policy/open-privacy-policy-link cofx)))

;; wallet modules

(handlers/register-handler-fx
 :wallet.settings.ui/currency-selected
 (fn [cofx [_ currency]]
   (currency-settings.models/set-currency cofx currency)))

;; chat module

(handlers/register-handler-fx
 :chat.ui/clear-history-pressed
 (fn [_ _]
   {:ui/show-confirmation {:title (i18n/label :t/clear-history-title)
                           :content (i18n/label :t/clear-history-confirmation-content)
                           :confirm-button-text (i18n/label :t/clear-history-action)
                           :on-accept #(re-frame/dispatch [:clear-history])}}))

(handlers/register-handler-fx
 :chat.ui/delete-chat-pressed
 (fn [_ [_ chat-id]]
   {:ui/show-confirmation {:title (i18n/label :t/delete-chat-confirmation)
                           :content ""
                           :confirm-button-text (i18n/label :t/delete-chat-action)
                           :on-accept #(re-frame/dispatch [:remove-chat-and-navigate-home chat-id])}}))

;; signal module

(handlers/register-handler-fx
 :signals/signal-received
 (fn [cofx [_ event-str]]
   (log/debug :event-str event-str)
   (signals/process cofx event-str)))

;; protocol module

(handlers/register-handler-fx
 :protocol.ui/close-app-confirmed
 (fn [cofx _]
   (protocol/handle-close-app-confirmed cofx)))

(handlers/register-handler-fx
 :protocol/state-sync-timed-out
 (fn [cofx _]
   (protocol/check-sync-state cofx)))

;; web3 module

(handlers/register-handler-fx
 :web3.callback/get-syncing-success
 (fn [cofx [_ error sync]]
   (protocol/update-sync-state cofx error sync)))

;; notifications module

(handlers/register-handler-fx
 :notifications/notification-event-received
 (fn [cofx [_ event]]
   (notifications/handle-push-notification cofx event)))

(handlers/register-handler-fx
 :notifications.callback/notification-stored
 (fn [cofx _]
   (accounts.login/user-login cofx)))

(handlers/register-handler-fx
 :notifications.callback/get-fcm-token-success
 (fn [{:keys [db]} [_ fcm-token]]
   {:db (assoc-in db [:notifications :fcm-token] fcm-token)}))

(handlers/register-handler-fx
 :notifications.callback/request-notifications-permissions-granted
 (fn [cofx _]
   (accounts/show-mainnet-is-default-alert cofx)))

(handlers/register-handler-fx
 :notifications.callback/request-notifications-permissions-denied
 (fn [cofx _]
   (accounts/show-mainnet-is-default-alert cofx)))

;; hardwallet module

(handlers/register-handler-fx
 :hardwallet.callback/check-nfc-support-success
 (fn [cofx [_ supported?]]
   (hardwallet/set-nfc-support cofx supported?)))

(handlers/register-handler-fx
 :hardwallet.callback/check-nfc-enabled-success
 (fn [cofx [_ enabled?]]
   (hardwallet/set-nfc-enabled cofx enabled?)))

(handlers/register-handler-fx
 :hardwallet.ui/status-hardwallet-option-pressed
 (fn [cofx _]
   (hardwallet/navigate-to-connect-screen cofx)))

(handlers/register-handler-fx
 :hardwallet.ui/password-option-pressed
 (fn [cofx _]
   (accounts.create/navigate-to-create-account-screen cofx)))

(handlers/register-handler-fx
 :hardwallet.ui/go-to-settings-button-pressed
 (fn [_ _]
   {:hardwallet/open-nfc-settings nil}))

(handlers/register-handler-fx
 :hardwallet.ui/connect-info-button-pressed
 (fn [cofx _]
   (browser/open-url cofx "https://hardwallet.status.im")))

(handlers/register-handler-fx
 :hardwallet.ui/hold-card-button-pressed
 (fn [{:keys [db] :as cofx} _]
   (fx/merge cofx
             {:db (assoc-in db [:hardwallet :setup-step] :begin)}
             (navigation/navigate-to-cofx :hardwallet-setup nil))))

(handlers/register-handler-fx
 :hardwallet.ui/begin-setup-button-pressed
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:hardwallet :setup-step] :prepare)}))

(handlers/register-handler-fx
 :hardwallet/connection-error
 (fn [_ _]
   {:utils/show-popup {:title      (i18n/label :t/cant-read-card)
                       :content    (i18n/label :t/cant-read-card-error-explanation)
                       :on-dismiss #(re-frame/dispatch [:hardwallet.ui/connection-error-confirm-button-pressed])}}))

(handlers/register-handler-fx
 :hardwallet.ui/connection-error-confirm-button-pressed
 (fn [{:keys [db] :as cofx} _]
   (fx/merge cofx
             {:db (assoc-in db [:hardwallet :setup-step] :begin)}
             (navigation/navigate-to-cofx :hardwallet-setup nil))))

(handlers/register-handler-fx
 :hardwallet.ui/secret-keys-next-button-pressed
 (fn [_ _]
   {:ui/show-confirmation {:title               (i18n/label :t/secret-keys-confirmation-title)
                           :content             (i18n/label :t/secret-keys-confirmation-text)
                           :confirm-button-text (i18n/label :t/secret-keys-confirmation-confirm)
                           :cancel-button-text  (i18n/label :t/secret-keys-confirmation-cancel)
                           :on-accept           #(re-frame/dispatch [:hardwallet.ui/secret-keys-dialog-confirm-pressed])
                           :on-cancel           #()}}))

(handlers/register-handler-fx
 :hardwallet.ui/secret-keys-dialog-confirm-pressed
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:hardwallet :setup-step] :complete)}))

(handlers/register-handler-fx
 :hardwallet.ui/success-button-pressed
 (fn [cofx _]
   (navigation/navigate-to-cofx cofx :home nil)))

(handlers/register-handler-fx
 :hardwallet.ui/pin-numpad-button-pressed
 (fn [cofx [_ number step]]
   (hardwallet/process-pin-input cofx number step)))

(handlers/register-handler-fx
 :hardwallet.ui/pin-numpad-delete-button-pressed
 (fn [{:keys [db]} [_ step]]
   (when-not (empty? (get-in db [:hardwallet :pin step]))
     {:db (update-in db [:hardwallet :pin step] pop)})))

;; browser module

(handlers/register-handler-fx
 :browser.ui/browser-item-selected
 (fn [cofx [_ browser-id]]
   (browser/open-existing-browser cofx browser-id)))

(handlers/register-handler-fx
 :browser.ui/url-input-pressed
 (fn [cofx _]
   (browser/update-browser-option cofx :url-editing? true)))

(handlers/register-handler-fx
 :browser.ui/url-input-blured
 (fn [cofx _]
   (browser/update-browser-option cofx :url-editing? false)))

(handlers/register-handler-fx
 :browser.ui/url-submitted
 (fn [cofx [_ url]]
   (browser/open-url-in-current-browser cofx url)))

(handlers/register-handler-fx
 :browser.ui/message-link-pressed
 (fn [cofx [_ link]]
   (browser/handle-message-link cofx link)))

(handlers/register-handler-fx
 :browser.ui/remove-browser-pressed
 (fn [cofx [_ browser-id]]
   (browser/remove-browser cofx browser-id)))

(handlers/register-handler-fx
 :browser.ui/lock-pressed
 (fn [cofx [_ secure?]]
   (browser/update-browser-option cofx :show-tooltip (if secure? :secure :not-secure))))

(handlers/register-handler-fx
 :browser.ui/close-tooltip-pressed
 (fn [cofx _]
   (browser/update-browser-option cofx :show-tooltip nil)))

(handlers/register-handler-fx
 :browser.ui/previous-page-button-pressed
 (fn [cofx _]
   (browser/navigate-to-previous-page cofx)))

(handlers/register-handler-fx
 :browser.ui/next-page-button-pressed
 (fn [cofx _]
   (browser/navigate-to-next-page cofx)))

(handlers/register-handler-fx
 :browser/navigation-state-changed
 (fn [cofx [_ event error?]]
   (browser/navigation-state-changed cofx event error?)))

(handlers/register-handler-fx
 :browser/bridge-message-received
 (fn [cofx [_ message]]
   (browser/process-bridge-message cofx message)))

(handlers/register-handler-fx
 :browser/error-occured
 (fn [cofx _]
   (browser/handle-browser-error cofx)))

(handlers/register-handler-fx
 :browser/loading-started
 (fn [cofx _]
   (browser/update-browser-option cofx :error? false)))

(handlers/register-handler-fx
 :browser.callback/resolve-ens-multihash-success
 (fn [cofx [_ hash]]
   (browser/resolve-ens-multihash-success cofx hash)))

(handlers/register-handler-fx
 :browser.callback/resolve-ens-multihash-error
 (fn [cofx _]
   (browser/update-browser-option cofx :resolving? false)))

(handlers/register-handler-fx
 :browser.callback/call-rpc
 (fn [cofx [_ message]]
   (browser/send-to-bridge cofx message)))

(handlers/register-handler-fx
 :browser.permissions.ui/dapp-permission-allowed
 (fn [cofx [_ dapp-name permission]]
   (browser.permissions/allow-permission cofx dapp-name permission)))

(handlers/register-handler-fx
 :browser.permissions.ui/dapp-permission-denied
 (fn [cofx [_ dapp-name]]
   (browser.permissions/process-next-permission cofx dapp-name)))

(handlers/register-handler-fx
 :browser.ui/open-in-status-option-selected
 (fn [cofx [_ url]]
   (browser/open-url cofx url)))

(handlers/register-handler-fx
 :browser.ui/open-dapp-button-pressed
 (fn [cofx [_ dapp-url]]
   (browser/open-url cofx dapp-url)))

(handlers/register-handler-fx
 :browser.ui/dapp-url-submitted
 (fn [cofx [_ dapp-url]]
   (browser/open-url cofx dapp-url)))

(handlers/register-handler-fx
 :browser.ui/open-modal-chat-button-pressed
 (fn [cofx [_ host]]
   (browser/open-chat-from-browser cofx host)))
