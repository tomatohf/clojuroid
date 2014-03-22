(ns com.qiaobujianli.clojuroid.FetcherService
	(:gen-class
		:extends android.app.IntentService
		:init init
		:constructors {[] [String]})
	(:use
		[com.qiaobujianli.util]
		[com.qiaobujianli.clojuroid.client :only [get-counts]]
		[com.qiaobujianli.clojuroid.database :only
			[new-database-helper close-database-helper save-counts]])
	(:import
		[android.content Context Intent]
		[android.os PowerManager PowerManager$WakeLock]
		[android.preference PreferenceManager]
		[com.qiaobujianli.clojuroid FetcherService]))


(def tag "Fetcher Service")

(defn ^PowerManager get-power-manager [^Context context]
	(.getSystemService context Context/POWER_SERVICE))

(def wake-locks (atom {}))

(defn new-wake-lock [^Context context]
	(let [lock-id (gensym "wake-lock-")]
		[lock-id
			(.newWakeLock
				(get-power-manager context)
				PowerManager/PARTIAL_WAKE_LOCK
				(str tag " - " lock-id))]))

(defn acquire-wake-lock [^Context context]
	(let [[lock-id ^PowerManager$WakeLock lock] (new-wake-lock context)]
		(swap! wake-locks assoc lock-id lock)
		(doto lock
			(.setReferenceCounted false)
			(.acquire))
		lock-id))

(defn release-wake-lock [lock-id-string]
	(let [lock-id (symbol lock-id-string)
			lock ^PowerManager$WakeLock (@wake-locks lock-id)]
		(unless (nil? lock)
			(.release lock)
			(swap! wake-locks dissoc lock-id))))

(defn fetch [^Context context http-client database-helper]
	(let [counts (get-counts http-client)]
		(unless (nil? counts) (save-counts database-helper counts))
		counts))

(defn fetch-once [^Context context]
	(let [http-client (new-http-client) database-helper (new-database-helper context)]
		(fetch context http-client database-helper)
		(shutdown-http-client http-client)
		(close-database-helper database-helper)))


(defn -init [] [[tag]])

(defn -onHandleIntent [^FetcherService this ^Intent intent]
	(fetch-once this)
	(let [lock-id-string (.getStringExtra intent "lock-id")]
		(unless (nil? lock-id-string)
			(release-wake-lock lock-id-string))))
