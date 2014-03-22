(ns com.qiaobujianli.clojuroid.DashboardPreference
	(:gen-class
		:extends android.preference.PreferenceActivity
		:implements [android.content.SharedPreferences$OnSharedPreferenceChangeListener]
		:exposes-methods {
			onCreate onCreateSuper
			onResume onResumeSuper
			onPause onPauseSuper
			onOptionsItemSelected onOptionsItemSelectedSuper})
	(:use
		[com.qiaobujianli.util]
		[com.qiaobujianli.clojuroid.Fetcher :only [check-auto-fetch]])
	(:import
		[android.os Bundle]
		[android.content Intent]
		[android.view MenuItem]
		[com.qiaobujianli.clojuroid DashboardPreference R$xml R$string]))


(defn -onCreate [^DashboardPreference this ^Bundle savedInstanceState]
	(.onCreateSuper this savedInstanceState)
	(.. this getActionBar (setDisplayHomeAsUpEnabled true))
	(.addPreferencesFromResource this R$xml/dashboard_preferences))

(defn -onOptionsItemSelected [^DashboardPreference this ^MenuItem item]
	(condp = (.getItemId item)
		android.R$id/home
		(do-return-true
			(let [intent (.. this getPackageManager (getLaunchIntentForPackage (.getPackageName this)))]
				(.addFlags intent Intent/FLAG_ACTIVITY_CLEAR_TOP)
				(.startActivity this intent)))
		(.onOptionsItemSelectedSuper this item)))

(defn -onResume [^DashboardPreference this]
	(.onResumeSuper this)
	(.. this
		getPreferenceScreen
		getSharedPreferences
		(registerOnSharedPreferenceChangeListener this)))

(defn -onPause [^DashboardPreference this]
	(.onPauseSuper this)
	(.. this
		getPreferenceScreen
		getSharedPreferences
		(unregisterOnSharedPreferenceChangeListener this)))

(defn -onSharedPreferenceChanged [^DashboardPreference this preferences key]
	(if (= key (.. this getResources (getString R$string/preference_key_auto)))
		(check-auto-fetch this)))
