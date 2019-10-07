(ns finance-clash.interceptors.cors
  (:require
   [clojure.spec.alpha :as s]
   [ring.middleware.cors :as cors]))

(s/def ::allow-origin (s/coll-of s/regex? :kind vector?))
(s/def ::allow-methods (s/coll-of keyword? :kind set?))
(s/def ::allow-credentials boolean?)
(s/def ::allow-headers (s/coll-of string? :kind set?))
(s/def ::expose-headers (s/coll-of string? :kind set?))
(s/def ::max-age nat-int?)
(s/def ::access-control
  (s/keys :opt-un [::allow-origin ::allow-methods ::allow-credentials
                   ::allow-headers ::expose-headers ::max-age]))

(s/def ::cors-interceptor (s/keys :opt-un [::access-control]))

(defn cors-interceptor-enter [access-control]
  (fn [ctx]
    (let [request (:request ctx)
          resp (cors/add-access-control request access-control
                                        cors/preflight-complete-response)]
      (if (and (cors/preflight? request)
               (cors/allow-request? request access-control))
        (assoc ctx :response resp)
        ctx))))

(defn cors-interceptor-leave [access-control]
  (fn [ctx]
    (let [request (:request ctx)
          response (:response ctx)]
      (if (and (cors/origin request) (cors/allow-request? request access-control))
        (assoc ctx :response
               (cors/add-access-control request access-control response))
        ctx))))

(defn cors-interceptor []
  {:name ::cors
   :spec ::access-control
   :compile
   (fn [{:keys [access-control] :as ctx} _]
     (when access-control
       (let [access-control (cors/normalize-config
                             (mapcat identity access-control))]
         {:enter (cors-interceptor-enter access-control)
          :leave (cors-interceptor-leave access-control)})))})
