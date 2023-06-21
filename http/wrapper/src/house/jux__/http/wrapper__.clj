(ns house.jux--.http.wrapper--
  (:require
   [house.jux--.http.wrapper--.cors :refer [wrap-cors]]
   [house.jux--.http.wrapper--.exceptions :refer [wrap-exceptions]]
   [house.jux--.http.wrapper--.github-webhook :refer [wrap-redeploy!]]
   [house.jux--.http.wrapper--.pprint :refer [wrap-pprint]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

#_(defn- wrap-keyword-params-if-necessary [delegate keyword-params?]
    (cond-> delegate
      keyword-params? wrap-keyword-params))

(defn wrap [{:keys [handler service-name on-redeploy cors-prefixes keyword-params?]}]
  (-> handler
      #_(wrap-keyword-params-if-necessary keyword-params?)
      #_(wrap-cors cors-prefixes)
      #_(wrap-redeploy! service-name on-redeploy)
      #_(wrap-exceptions)
      (wrap-pprint)
      #_(wrap-params)))
