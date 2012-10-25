(ns aws.sdk.ec2
  "Functions to access the Amazon EC2 compute service.

  Each function takes a map of credentials as its first argument. The
  credentials map should contain an :access-key key and a :secret-key key."
  (:import com.amazonaws.auth.BasicAWSCredentials
           com.amazonaws.services.ec2.AmazonEC2Client
           com.amazonaws.AmazonServiceException
           com.amazonaws.services.ec2.model.DescribeImagesRequest 
           com.amazonaws.services.ec2.model.DescribeInstancesRequest
           com.amazonaws.services.ec2.model.Filter
           com.amazonaws.services.ec2.model.GroupIdentifier
           com.amazonaws.services.ec2.model.Instance
           com.amazonaws.services.ec2.model.InstanceState
           com.amazonaws.services.ec2.model.Placement
           com.amazonaws.services.ec2.model.Reservation
           com.amazonaws.services.ec2.model.Tag))

(defn- ec2-client*
  "Create an AmazonEC2Client instance from a map of credentials."
  [cred]
  (AmazonEC2Client.
   (BasicAWSCredentials.
    (:access-key cred)
    (:secret-key cred))))

(def ^{:private true}
  ec2-client
  (memoize ec2-client*))


;;
;; convert object graphs to clojure maps
;;

(defprotocol ^{:no-doc true} Mappable
  "Convert a value into a Clojure map."
  (^{:no-doc true} to-map [x] "Return a map of the value."))


;;
;; filters
;;

(defn aws-filter
  "Returns a Filter that can be used in calls to AWS to limit the results returned.

  E.g. (ec2/aws-filter \"tag:Name\" \"my-instance\")"
  [name & values]
  [(Filter. name values)])

(defn instance-filter
  "Returns a filter that can be used with ec2/describe-instances. It
  should be passed a Filter created by ec2/aws-filter."
  [filter]
  (.withFilters (DescribeInstancesRequest.) filter))

(defn instance-id-filter
  "Returns an instance filter that can be passed to ec2/describe-instances to describe a single instance."
  [id]
  (instance-filter (aws-filter "instance-id" id)))


;;
;; tags
;;

(defn- keywordify
  "Generates a lower case keyword from an arbirtary string."
  [s]
  (keyword (clojure.string/replace (clojure.string/lower-case s) "_" "-")))


;;
;; instances
;;

(extend-protocol Mappable
  Tag
  (to-map [tag]
    {(keywordify (.getKey tag)) (.getValue tag)})

  InstanceState
  (to-map [instance-state]
    {:name (.getName instance-state)
     :code (.getCode instance-state)})

  Placement
  (to-map [placement]
    {:availability-zone (.getAvailabilityZone placement)
     :group-name        (.getGroupName placement)
     :tenancy           (.getTenancy placement)})

  Instance
  (to-map [instance]
    {:id                (.getInstanceId instance)
     :state             (to-map (.getState instance))
     :type              (.getInstanceType instance)
     :placement         (to-map (.getPlacement instance))
     :tags              (reduce merge (map to-map (.getTags instance)))
     :image             (.getImageId instance)
     :launch-time       (.getLaunchTime instance)})

  GroupIdentifier
  (to-map [group-identifier]
    {:id   (.getGroupId group-identifier)
     :name (.getGroupName group-identifier)})

  Reservation
  (to-map [reservation]
    {:instances   (map to-map (.getInstances reservation))
     :group-names (flatten (.getGroupNames reservation))
     :groups      (map to-map (.getGroups reservation))})

  nil
  (to-map [_] nil))


(defn describe-instances
  "List all the EC2 instances for the supplied credentials, applying the optional filter if supplied.

  Returns a list of Reservations, a data structure which contains the following keys:
    :instances   - a list of Instances
    :groups      - a list of security groups requested for the instances in this reservation
    :group-names - a list of security group names requested for the instances in this reservation"
  ([cred]
     (map to-map (.getReservations (.describeInstances (ec2-client cred)))))
  ([cred filter]
     (map to-map (.getReservations (.describeInstances (ec2-client cred) filter)))))
