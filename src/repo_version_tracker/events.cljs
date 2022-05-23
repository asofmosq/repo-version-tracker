(ns repo-version-tracker.events
  (:require
    [ajax.core :as ajax]
    [repo-version-tracker.common :as common]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

;;todo: need to organize events + subscriptions
;;todo: should i use namespaces?

(def initial-db
  {
   :current-user nil
   :current-repo nil
   :watched-repos {}
   :suggested-repos {}
   :loading? false
   })

(def repo-keys
  #{:full_name :description :html_url :releases_url :updated_at :archived :language})

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::get-data :current-user)]
  (fn [{:keys [local-store]}_]
      {:db (assoc initial-db :current-user (:current-user local-store))}))

(rf/reg-event-fx
  ::initialize-repos-view
  [(rf/inject-cofx ::get-user-repos)]
  (fn [{:keys [db local-store]} _]
      {:db (assoc db :watched-repos (:watched-repos local-store))}))

(rf/reg-cofx
  ::get-data
  (fn [cofx key]
      (assoc cofx :local-store {key (common/get-item key)})))

(rf/reg-cofx
  ::get-user-repos
  (fn [{:keys [db] :as cofx} _]
      (assoc cofx :local-store {:watched-repos (common/get-item (:current-user db))})))

(rf/reg-event-fx
  ::remove-data!
  (fn [_ [_ key]]
      (common/remove-item! key)
      {}))

(rf/reg-event-fx
  ::set-data!
  (fn [_ [_ key value]]
      (common/set-item! key value)
      {}))

(rf/reg-sub
  ::loading?
  (fn [db _]
      (:loading? db)))

(rf/reg-sub
  ::get-user
  (fn [db _]
      (:current-user db)))

(rf/reg-event-fx
  ::login
  (fn [{:keys [db]} [_ user-id]]
      {:db (assoc db :current-user user-id)
       :fx [[:dispatch [::set-data! :current-user user-id]]
            [:dispatch [::fetch-repos]]]}))

(rf/reg-event-fx
  ::logout
  (fn [{:keys [db]} [_ user-id]]
      {:db (assoc db :current-user nil)
       :fx [[:dispatch [::set-data! :current-user nil]]]}))

(rf/reg-sub
  ::get-watched-repos
  (fn [db _]
      (:watched-repos db)))

(rf/reg-sub
  :get-sorted-watched-repos
  (fn [db _]
    (sort-by :full-name (:watched-repos db))))

(rf/reg-sub
  ::get-repo-suggestions
  (fn [db _]
      (:suggested-repos db)))

(rf/reg-event-fx
  ::watch-repo
  (fn [{:keys [db]} [_ {:keys [full_name] :as repo}]]
      {:db (assoc-in db [:watched-repos full_name] repo)
       :fx [[:dispatch [::save-repo]]]}))

(rf/reg-event-fx
  ::save-repo
  (fn [{:keys [db]} _]
      (common/set-item! (:current-user db) (pr-str (:watched-repos db)))
      {}))

;;todo: this may need to dispatch a separate ::set-data! event
;;todo: ensure unwatch works
(rf/reg-event-fx
  ::unwatch-repo
  (fn [{:keys [db]} [_ repo]]
      {:db (update db :watched-repos dissoc repo)
       :fx [[:dispatch [::set-data! (:current-user db) (pr-str (:watched-repos db))]]]}))

(rf/reg-event-db
  ::clear-search-suggestions
  (fn [db _]
      (assoc db :suggested-repos [])))

(rf/reg-event-db
  ::update-repo-suggestions
  (fn [db [_ {:keys [items]}]]
      (assoc db :suggested-repos (map #(select-keys % repo-keys) items))))

;;todo: improve error message
(rf/reg-event-fx
  ::search-repos-failure
  (fn [{:keys [db]} _]
      {:db (assoc db :loading? false)
       :show-message [:error "Search failed. Please try again."]}))

(rf/reg-event-fx
  ::search-repos
  (fn [{:keys [db]} [_ search-string]]
      {:db (assoc db :loading? true)
       :http-xhrio {:method :get
                    :uri "https://api.github.com/search/repositories"
                    :params {:q search-string :per-page 5}
                    :timeout 5000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::update-repo-suggestions]
                    :on-failure [::search-repos-failure]}}))

(rf/reg-event-fx
  ::load-repo-failure
  (fn [{:keys [db]} _]
      :show-message "Failed to repo. Please try again later."))

(rf/reg-event-fx
  ::load-repo-success
  (fn [{:keys [db]} [_ repo-name]]
      {:db (update db :watched-repos repo-name)}))

;;todo: not currently using these
(rf/reg-event-fx
  ::load-repo
  (fn [{:keys [db]} [_ repo-id]]
      {:db (assoc db :current-repo repo-id)
       :http-xhrio {:method :get
                    :uri "https://api.github.com/repos/" repo-id "/releases"
                    :timeout 5000
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::load-repo-success]
                    :on-failure [::load-repo-failure]}}))

;;todo!
(rf/reg-sub
  ::fetch-repos)

