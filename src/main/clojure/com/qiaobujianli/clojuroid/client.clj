(ns com.qiaobujianli.clojuroid.client
	(:use [com.qiaobujianli.util]))

(def host "http://qiaobujianli.com")

(defn full-url [url] (str host url))

(defn extract-between
	([text left center right] (re-find (re-pattern (str "(?sm)" left center right)) text))
	([text left right] (extract-between text left ".+?" right)))

(defn get-counts [http-client]
	(try
		(let [extract-count
				(fn [url]
					(extract-between
						(http-get http-client (full-url url))
						(str "<span[^<>]+class=\"span1\"[^<>]*>[^\\d]*") "(\\d+)" ".*</span>"))
				[_ recruitment] (extract-count "/recruitment/index")
				[_ campus] (extract-count "/campus-talk/index")]
			(map #(read-string %) [recruitment campus]))
		(catch Exception e
			(log-exception "Failed to get-counts !" e)
			nil)))
