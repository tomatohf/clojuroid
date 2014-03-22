(ns com.qiaobujianli.clojuroid.Dashboard
	(:require
		[com.qiaobujianli.clojuroid DashboardPreference])
	(:gen-class
		:extends android.app.Activity
		:exposes-methods {
			onCreate onCreateSuper
			onDestroy onDestroySuper
			onCreateOptionsMenu onCreateOptionsMenuSuper
			onOptionsItemSelected onOptionsItemSelectedSuper}
		:state state
		:init init)
	(:use
		[com.qiaobujianli.util]
		[com.qiaobujianli.clojuroid.FetcherService :only [fetch]]
		[com.qiaobujianli.clojuroid.Fetcher :only [check-auto-fetch]]
		[com.qiaobujianli.clojuroid.database :only
			[new-database-helper close-database-helper
				counts-for-list counts-for-chart
				column-name-time
				column-name-year column-name-month column-name-date
				column-name-recruitment column-name-campus]])
	(:import
		[java.util Calendar Date]
		[java.text SimpleDateFormat]
		[android.os Bundle]
		[android.content Intent]
		[android.widget ListView SimpleCursorAdapter]
		[android.view Menu MenuItem ViewGroup View$OnClickListener]
		[org.achartengine ChartFactory]
		[org.achartengine.model XYMultipleSeriesDataset XYSeries]
		[com.qiaobujianli.clojuroid
			Dashboard DashboardPreference
			R$layout R$id R$menu R$string]))


(defn get-state [^Dashboard this] (.state this))

(defn get-state-value [^Dashboard this key] (@(get-state this) key))

(defn refresh-list [^Dashboard this ^ListView list]
	(.setAdapter list
		(SimpleCursorAdapter.
			this R$layout/row (counts-for-list (get-state-value this :database))
			(into-array [column-name-time column-name-recruitment column-name-campus])
			(int-array [R$id/time R$id/recruitment R$id/campus]))))

(defn refresh-chart [^Dashboard this]
	(let [resources (.getResources this)
			dataset (XYMultipleSeriesDataset.)
			recruitment-series (XYSeries. (.getString resources R$string/title_recruitment))
			campus-series (XYSeries. (.getString resources R$string/title_campus))
			cursor (counts-for-chart (get-state-value this :database))
			get-column #(.getInt cursor (.getColumnIndex cursor %))
			total-days (atom 0)
			last-counts (atom {})]
		(.moveToLast cursor)
		(dotimes
			[i (.getCount cursor)]
			(let [calendar (Calendar/getInstance)
					x
					(do
						(doto calendar
							(.set Calendar/YEAR (get-column column-name-year))
							(.set Calendar/MONTH (- (get-column column-name-month) 1))
							(.set Calendar/DAY_OF_MONTH (get-column column-name-date))
							(.set Calendar/HOUR_OF_DAY 0)
							(.set Calendar/MINUTE 0)
							(.set Calendar/SECOND 0)
							(.set Calendar/MILLISECOND 0))
						(.getTimeInMillis calendar))
					recruitment-count (double (get-column column-name-recruitment))
					campus-count (double (get-column column-name-campus))
					last-recruitment-count (@last-counts :recruitment)
					last-campus-count (@last-counts :campus)
					last-x (@last-counts :x)]
				(if (and last-recruitment-count last-campus-count last-x)
					(let [days (-> x (- last-x) (/ MILLISECONDS_IN_DAY) int)
							valid-days (if (< days 1) 1 days)
							calculate-count #(-> %1 (- %2) (/ valid-days) int)]
						(swap! total-days + valid-days)
						(.add recruitment-series x (calculate-count recruitment-count last-recruitment-count))
						(.add campus-series x (calculate-count campus-count last-campus-count))))
				(reset! last-counts {:recruitment recruitment-count, :campus campus-count, :x x}))
			(.moveToPrevious cursor))
		(.close cursor)
		(doto dataset
			(.addSeries recruitment-series)
			(.addSeries campus-series))
		(doto ^ViewGroup (.findViewById this R$id/chart)
			(.removeAllViews)
			(.addView
				(let [
						chart-view
						(ChartFactory/getTimeChartView
							this
							dataset
							(.. ^Class (resolve 'com.qiaobujianli.clojuroid.ChartRenderer)
								(getConstructor (into-array [String Integer/TYPE]))
								(newInstance
									(to-array
										[(.getString resources R$string/title_chart)
										(int @total-days)])))
							"M-d")
						context this]
					(.setOnClickListener
						chart-view
						(proxy [View$OnClickListener] []
							(onClick [view]
								(let [series-selection (.getCurrentSeriesAndPoint chart-view)]
									(unless (nil? series-selection)
										(show-toast
											context
											(.getString
												resources
												(if (< 0 (.getSeriesIndex series-selection))
													R$string/toast_text_point_campus
													R$string/toast_text_point_recruitment)
												(to-array
													[(.format
														(SimpleDateFormat. "yyyy-M-d")
														(Date. (long (.getXValue series-selection))))
													(.getValue series-selection)]))))))))
					chart-view)))))

(defn try-view [^Dashboard this view-id f]
	(let [view (.findViewById this view-id)]
		(if (nil? view) false (do-return-true (f view)))))

(defn refresh-view [^Dashboard this]
	(unless
		(try-view this R$id/list (partial refresh-list this))
		(refresh-chart this)))

(defn fetch-counts [^Dashboard this]
	(let [resources (.getResources this)]
		(execute-async-task
			this,
			nil
			(.getString resources R$string/dialog_fetch_message)
			(fn [dialogs]
				(fetch
					this
					(get-state-value this :client)
					(get-state-value this :database)))
			(fn [result]
				(if (nil? result)
					(show-toast this (.getString resources R$string/toast_text_fail))
					(refresh-view this))))))


(defn -init [] [[] (atom {})])

(defn -onDestroy [^Dashboard this]
	(.onDestroySuper this)
	(shutdown-http-client (get-state-value this :client))
	(close-database-helper (get-state-value this :database)))

(defn -onCreate [^Dashboard this ^Bundle savedInstanceState]
	(.onCreateSuper this savedInstanceState)
	(reset! (get-state this) {
		:client (new-http-client),
		:database (new-database-helper this)})
	(.setContentView this R$layout/dashboard)
	(refresh-view this)
	(check-auto-fetch this))

(defn -onCreateOptionsMenu [^Dashboard this ^Menu menu]
	(.onCreateOptionsMenuSuper this menu)
	(.. this getMenuInflater (inflate R$menu/dashboard menu))
	true)

(defn -onOptionsItemSelected [^Dashboard this ^MenuItem item]
	(condp = (.getItemId item)
		R$id/menu_add
		(do-return-true (fetch-counts this))
		R$id/menu_refresh
		(do-return-true
			(refresh-view this)
			(show-toast this (.getString (.getResources this) R$string/toast_text_refresh)))
		R$id/menu_setting
		(do-return-true
			(.startActivity this (Intent. this DashboardPreference)))
		(.onOptionsItemSelectedSuper this item)))
