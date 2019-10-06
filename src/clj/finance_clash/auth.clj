(ns finance-clash.auth
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.sign.jwt :as jwt]
   [clojure.pprint :refer (pprint)]))

(def secret "mysupersecret")

(def protected-interceptor
  {:enter
   (fn [ctx]
     (if-not (authenticated? (:request ctx))
       (assoc ctx :queue [] :response {:status 401 :body "Unauthorized"})
       ctx))})

(defn sign-user [user]
  (jwt/sign {:user (:id user)} secret {:alg :hs512}))

(defn unsign-user [token]
  (jwt/unsign token secret {:alg :hs512}))
