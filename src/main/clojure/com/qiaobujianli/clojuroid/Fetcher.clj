(ns com.qiaobujianli.clojuroid.Fetcher
	(:require
		[com.qiaobujianli.clojuroid FetcherService])
	(:gen-class
		:extends android.content.BroadcastReceiver)
	(:use
		[com.qiaobujianli.clojuroid.FetcherService :only [acquire-wake-lock]]
		[com.qiaobujianli.util :only [MILLISECONDS_IN_HOUR]])
	(:import
		[java.util Calendar]
		[android.content Context Intent]
		[android.preference PreferenceManager]
		[android.app AlarmManager PendingIntent]
		[com.qiaobujianli.clojuroid Fetcher FetcherService R$string]))


(defn ^AlarmManager get-alarm-manager [^Context context]
	(.getSystemService context Context/ALARM_SERVICE))

(defn get-pending-intent [^Context context]
	(PendingIntent/getBroadcast context 1 (Intent. context Fetcher) 0))

(defn cancel-auto-fetch [^Context context]
	(.cancel (get-alarm-manager context) (get-pending-intent context)))

(def interval-in-hour 6)

(defn auto-fetch [^Context context]
	(.setRepeating
		(get-alarm-manager context)
		AlarmManager/RTC_WAKEUP
		(.getTimeInMillis
			(let [calendar (Calendar/getInstance)]
				(doto calendar
					(.set
						Calendar/HOUR_OF_DAY
						(->
							(.get calendar Calendar/HOUR_OF_DAY)
							(/ interval-in-hour)
							int
							(+ 1)
							(* interval-in-hour)
							(- 1)))
					(.set Calendar/MINUTE 59)
					(.set Calendar/SECOND 0)
					(.set Calendar/MILLISECOND 0))
				(if (.before calendar (Calendar/getInstance))
					(.add calendar Calendar/HOUR_OF_DAY interval-in-hour))
				calendar))
		(* interval-in-hour MILLISECONDS_IN_HOUR)
		(get-pending-intent context)))

(defn check-auto-fetch [^Context context]
	(if
		(.getBoolean
			(PreferenceManager/getDefaultSharedPreferences context)
			(.. context getResources (getString R$string/preference_key_auto))
			false)
		(auto-fetch context)
		(cancel-auto-fetch context)))


(defn -onReceive [this ^Context context intent]
	(let [lock-id (acquire-wake-lock context)
			service-intent (Intent. context FetcherService)]
		(.putExtra service-intent "lock-id" (str lock-id))
		(.startService context service-intent)))
