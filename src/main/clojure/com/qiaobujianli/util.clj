(ns com.qiaobujianli.util
	(:import
		[android.content Context DialogInterface$OnCancelListener]
		[android.widget Toast]
		[android.util Log]
		[android.os AsyncTask StrictMode StrictMode$ThreadPolicy$Builder]
		[android.app ProgressDialog]
		[org.apache.http.client HttpClient]
		[org.apache.http.impl.client DefaultHttpClient BasicResponseHandler]
		[org.apache.http.client.methods HttpGet HttpPost]
		[org.apache.http.client.entity UrlEncodedFormEntity]
		[org.apache.http.message BasicNameValuePair]))


(def MILLISECONDS_IN_HOUR (* 1000 60 60))
(def MILLISECONDS_IN_DAY (* 24 MILLISECONDS_IN_HOUR))

(defmacro unless [test & then]
	`(if (not ~test)
		(do ~@then)))

(defn between? [value start end]
	(and (>= value start) (<= value end)))

(defmacro do-return [value & body]
	`(do ~@body ~value))

(defn do-return-true [& body]
	(do-return true body))



(defn ^HttpClient new-http-client [] (DefaultHttpClient.))

(defn shutdown-http-client [^HttpClient http-client]
	(.. http-client getConnectionManager shutdown))

(defn http-get [^HttpClient http-client ^String url]
	(.execute http-client (HttpGet. url) (BasicResponseHandler.)))

(defn http-post [^HttpClient http-client ^String url params]
	(let [post (HttpPost. url)]
		(unless (empty? params)
			(.setEntity post
				(UrlEncodedFormEntity.
					(map #(let [[key value] %] (BasicNameValuePair. key value)) params))))
		(.execute http-client post (BasicResponseHandler.))))



(defn log-exception [msg ^Exception e]
	(Log/e "clojuroid" msg e))

(defn show-toast [^Context context ^String text]
	(.show (Toast/makeText context text Toast/LENGTH_LONG)))

(defn execute-async-task [^Context context ^String title message background ui]
	(let [loading (ProgressDialog. context)]
		(.execute
			(proxy [AsyncTask DialogInterface$OnCancelListener] []
				(onPreExecute []
					(doto loading
						(.setTitle title)
						(.setMessage message)
						(.setCancelable true)
						(.setOnCancelListener this)
						(.setIndeterminate true)
						(.show)))
				(onPostExecute [result] (.cancel loading) (ui result))
				(doInBackground [dialogs] (background dialogs))
				(onCancel [dialog] (.cancel ^AsyncTask this true)))
			(into-array [loading]))))

(defn disableStrictMode []
	(StrictMode/setThreadPolicy (.. (StrictMode$ThreadPolicy$Builder.) permitAll build)))
