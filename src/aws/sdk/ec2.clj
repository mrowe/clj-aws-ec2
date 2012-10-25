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
           com.amazonaws.services.ec2.model.Tag
           ))

(use '[clj-time.coerce :only (from-date)])

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
;; instances
;;

(extend-protocol Mappable
  Tag
  (to-map [tag]
    {:key   (.getKey tag)
     :value (.getValue tag)})

  InstanceState
  (to-map [instance-state]
    {:name (.getName instance-state)})

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
     :tags              (map to-map (.getTags instance))
     :image             (.getImageId instance)
     :launch-time       (from-date (.getLaunchTime instance))})

  GroupIdentifier
  (to-map [group-identifier]
    {:id   (.getGroupId group-identifier)
     :name (.getGroupName group-identifier)})

  Reservation
  (to-map [reservation]
    {:instances   (map to-map (.getInstances reservation))
     :group-names (.getGroupNames reservation)
     :groups      (map to-map (.getGroups reservation))})

  nil
  (to-map [_] nil))


(defn describe-instances
  "List all the EC2 instances for the supplied credentials."
  [cred]
  (map to-map (.getReservations (.describeInstances (ec2-client cred)))))
