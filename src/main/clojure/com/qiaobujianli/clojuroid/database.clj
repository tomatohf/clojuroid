(ns com.qiaobujianli.clojuroid.database
	(:require
		[clojure.string :as string])
	(:import
		[java.util Calendar]
		[android.database Cursor]
		[android.database.sqlite SQLiteOpenHelper SQLiteDatabase]
		[android.content Context ContentValues]))

(def database-name "clojuroid")
(def database-version 1)

(def column-name-year "year")
(def column-name-month "month")
(def column-name-date "date")
(def column-name-hour "hour")
(def column-name-minute "minute")
(def column-name-second "second")
(def column-name-recruitment "recruitment")
(def column-name-campus "campus")
(def column-name-time "_id")

(def table-name-counts "counts")
(def pk-columns-counts [
	column-name-year
	column-name-month
	column-name-date
	column-name-hour
	column-name-minute
	column-name-second])
(def columns-counts
	(concat pk-columns-counts [column-name-recruitment column-name-campus]))

(def ddl-counts
	(let [mk-string (fn [f columns] (string/join ", " (map f columns)))]
		(str
			"CREATE TABLE " table-name-counts " ("
				(mk-string #(str % " INTEGER NOT NULL") columns-counts)
			", PRIMARY KEY ("
				(mk-string #((constantly %)) pk-columns-counts)
			"));")))

(defn ^SQLiteOpenHelper new-database-helper [^Context context]
	(proxy [SQLiteOpenHelper] [context database-name nil database-version]
		(onCreate [^SQLiteDatabase db]
			(.execSQL db ddl-counts))))

(defn close-database-helper [^SQLiteOpenHelper database-helper]
	(.close database-helper))

(defn save-counts
	([^SQLiteOpenHelper database-helper counts ^Calendar time]
		(.. database-helper
			getWritableDatabase
			(replace
				table-name-counts
				nil
				(let [values (ContentValues.)]
					(doseq [
						[^String key ^Long value]
						(zipmap
							columns-counts
							(concat
								(map
									#(let [n (->> % (.get time) long)] (if (= (Calendar/MONTH) %) (+ 1 n) n))
									[(Calendar/YEAR)
										(Calendar/MONTH)
										(Calendar/DAY_OF_MONTH)
										(Calendar/HOUR_OF_DAY)
										(Calendar/MINUTE)
										(Calendar/SECOND)])
								counts))]
						(.put values key value))
					values))))
	([^SQLiteOpenHelper database-helper counts]
		(save-counts database-helper counts (Calendar/getInstance))))

(defn counts
	([^SQLiteOpenHelper database-helper limit]
		(.. database-helper
			getReadableDatabase
			(query
				table-name-counts
				(into-array columns-counts)
				nil nil nil nil
				(string/join ", " (map #(str % " DESC") pk-columns-counts))
				limit)))
	([^SQLiteOpenHelper database-helper] (counts database-helper 100)))

(defn counts-for-list
	([^SQLiteOpenHelper database-helper limit]
		(.. database-helper
			getReadableDatabase
			(rawQuery
				(str
					"SELECT "
						(apply
							#(string/join " || " [%1 "'-'" %2 "'-'" %3 "' '" %4 "':'" %5 "':'" %6])
							(map
								#(str "(CASE WHEN " % " < 10 THEN ('0' || " % ") ELSE " % " END)")
								pk-columns-counts))
						" AS " column-name-time ", " column-name-recruitment ", " column-name-campus
					" FROM " table-name-counts
					" ORDER BY " (string/join ", " (map #(str % " DESC") pk-columns-counts))
					" LIMIT " limit)
				nil)))
	([^SQLiteOpenHelper database-helper] (counts-for-list database-helper 100)))

(defn ^Cursor counts-for-chart
	([^SQLiteOpenHelper database-helper limit]
		(.. database-helper
			getReadableDatabase
			(rawQuery
				(let [time-columns (take 3 pk-columns-counts)]
					(str
						"SELECT "
							(string/join
								", "
								(into
									time-columns
									[(str "MAX(" column-name-recruitment ") AS " column-name-recruitment)
										(str "MAX(" column-name-campus ") AS " column-name-campus)]))
						" FROM " table-name-counts
						" GROUP BY " (string/join ", " time-columns)
						" ORDER BY " (string/join ", " (map #(str % " DESC") time-columns))
						" LIMIT " limit))
				nil)))
	([^SQLiteOpenHelper database-helper] (counts-for-chart database-helper (+ 31 1))))
