(ns com.qiaobujianli.clojuroid.FetcherChecker
	(:gen-class
		:extends android.content.BroadcastReceiver)
	(:use
		[com.qiaobujianli.clojuroid.Fetcher :only [check-auto-fetch]])
	(:import
		[android.content Context]))


(defn -onReceive [this ^Context context intent]
	(check-auto-fetch context))
