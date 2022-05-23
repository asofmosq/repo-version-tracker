(ns repo-version-tracker.core
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [re-frame.core :as rf]
    [repo-version-tracker.events :as events]
    [repo-version-tracker.common :as common]))

;;todo: is section__title the best class for this?
(defn login-page []
      (let [user (r/atom nil)]
           [:div.page
            [:h1.page__heading "Login"]
            [:section.section>div.container>div.content
             [:h3.section__title "Username"]
             [:div.control>input.input
              {:type :text
               :placeholder "Enter username"
               :on-change (fn [event]
                              (when-let [username (-> event .-target .-value)]
                                        (reset! user username)))}]
             [:br]
             [:div.control>button.button.is-primary
              {:on-click #(rf/dispatch [::events/login @user])}
              "Submit"]]]))

(defn navbar []
      [:nav.navbar.is-primary {:role "navigation"}
       [:div.navbar-menu
        [:div.navbar-start [:a.navbar-item {:href "/" :style {:font-weight :bold}} "Repo Version Tracker"]]
        (when-let [username @(rf/subscribe [::events/get-user])]
                  [:div.navbar-end
                   [:div.navbar-item
                    [:p.navbar-item {:style {:font-weight :bold}} "Logged in: "]
                    [:p.navbar-item username]
                    [:a.button.is-light {:on-click #(rf/dispatch [::events/logout])} "Logout"]]])]])

(defn search-suggestions []
      (when-let [repos (not-empty @(rf/subscribe [::events/get-repo-suggestions]))]
           [:div.dropdown-menu {:id "dropdown-menu" :role "menu"}
            [:div.dropdown-content
             (for [{:keys [full_name description html_url releases_url updated_at archived language]
                    :as repo} repos]
                  ^{:key full_name}
                  [:div.dropdown-item
                   [:button.button.watch-repo {:on-click #(rf/dispatch [::events/watch-repo repo])
                                               :title "Watch repo"}
                    [:i.fa-solid.fa-circle-plus]]
                   [:a.search-item {:href html_url :target :_blank} full_name]
                   [:hr.dropdown-divider]])]]))

(defn search []
      [:nav.panel
       [:p.panel-heading "Search GitHub Repositories"]
       [:div.dropdown.is-active
        [:div.dropdown-trigger
         [:div.field
          [:p.control.has-icons-right
           [:input.input
            {:type        :search
             :placeholder "Search..."
             :on-change   (common/debounce (fn [event]
                                               (let [search-string (-> event .-target .-value)]
                                                    (if (blank? search-string)
                                                      (rf/dispatch [::events/clear-search-suggestions])
                                                      (rf/dispatch [::events/search-repos search-string]))))
                                           200)
             :on-blur #(rf/dispatch [::events/clear-search-suggestions])
             }]
           [:span.icon.is-left [:i.fas.fa-search]]]]]
        [search-suggestions]]])

;;todo: need to format the time for the card
(defn card [{:keys [full_name description html_url releases_url updated_at archived language]
             :as repo}]
      [:div.card
       [:div.card-header
        [:p.card-header-title full_name]
        [:button.card-header-icon
         [:span.icon
          [:i.fas.fa-angle-down]]]]
       [:div.card-content
        [:div.content
         [:p description]
         [:p archived]
         [:p language]]]])

;;todo: delete loading? if i never use it
(defn repo-dashboard []
      (when-let [watched-repos (not-empty @(rf/subscribe [::events/get-watched-repos]))]
                [:div
                 (for [[full_name repo] watched-repos]
                      ^{:key full_name}
                      [card repo])]))

(defn repos-page []
      (r/create-class
        {:component-did-mount
         (fn [_]
             (rf/dispatch-sync [::events/initialize-repos-view]))
         :component-will-unmount
         (fn [_]
             (rf/dispatch [::events/clear-search-suggestions]))
         :reagent-render
         (fn []
             [:div.page
              [search]
              [repo-dashboard]])}))

;;todo: do I like these section div setup?
(defn home-page []
      [:div
       [navbar]
       [:section.section>div.container>div.content
        (if (some? @(rf/subscribe [::events/get-user]))
          [repos-page]
          [login-page])]])

(defn mount-components []
      (rdom/render [#'home-page] (.getElementById js/document "app")))

;;todo: diferente?
(defn ^:export init! []
      (rf/dispatch-sync [::events/initialize-db])
      (mount-components))





