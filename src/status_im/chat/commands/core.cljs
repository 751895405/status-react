(ns status-im.chat.commands.core
  (:require [status-im.chat.commands.protocol :as protocol]
            [status-im.chat.commands.impl.transactions :as transactions]
            [status-im.chat.models.input :as input]))

(def ^:private arg-wrapping-char "\"")
(def ^:private command-char "/")
(def ^:private space-chat " ")

(def commands-register
  "Register of all commands. Whenever implementing a new command,
  provide the implementation in the `status-im.chat.commands.impl.*` ns,
  and add its instance here."
  #{(transactions/PersonalSendCommand.)})

(defn validate-and-send
  "Validates and sends command in current chat"
  [command cofx]
  nil)

(defn send
  "Sends command with given arguments in particular chat"
  [command chat-id cofx]
  nil)

(defn set-command-argument
  "Set value as command argument for the current chat"
  [value {:keys [db]}]
  (let [{:keys [current-chat-id]} db
        [command & command-args]  (-> (get-in db [:chats current-chat-id :input-text])
                                      input/split-command-args)]))
