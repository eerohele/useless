(ns useless.server.renderer)


(defmulti render :type)


(defmethod render :default
  [{:keys [content]}]
  content)
