(ns wh.admin.tags.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub reg-sub-raw]]
            [wh.admin.tags.db :as tags]
            [wh.common.specs.tags :as tag-spec]
            [wh.common.text :as text]
            [wh.re-frame.subs :refer [<sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(reg-sub ::sub-db (fn [db _] (::tags/sub-db db)))

(reg-sub
  ::search-term
  :<- [::sub-db]
  (fn [sub-db _]
    (::tags/search-term sub-db)))

(reg-sub
  ::type-filter
  :<- [::sub-db]
  (fn [sub-db _]
    (::tags/type-filter sub-db)))

(reg-sub
  ::tag-types
  (fn [_ _]
    (map #(hash-map :id (name %) :label (str/capitalize (name %))) tag-spec/types)))

(reg-sub
  ::tag-subtypes
  (fn [_ [_ type]]
    (when-let [subtypes (case (keyword type)
                          :tech tag-spec/tech-subtypes
                          :benefit tag-spec/benefit-subtypes
                          nil)]
      (map #(hash-map :id (name %) :label (str/capitalize (name %))) subtypes))))

(reg-sub-raw
  ::all-tags
  (fn [_ _]
    (reaction
      (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags]))))

(reg-sub
  ::all-tags--filtered
  :<- [::all-tags]
  :<- [::type-filter]
  :<- [::search-term]
  (fn [[all-tags type-filter search-term] [_ page amount]]
    (let [filtered-tags (cond->> all-tags
                                 type-filter
                                 (filter (comp #{type-filter} :type))
                                 (text/not-blank search-term)
                                 (filter #(str/includes? (str/lower-case (:label %)) (str/lower-case search-term))))]
      {:tags (->> filtered-tags
                  (drop (* (dec page) amount))
                  (take amount))
       :total (count filtered-tags)})))
