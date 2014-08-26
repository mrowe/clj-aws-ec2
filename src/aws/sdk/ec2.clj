(ns aws.sdk.ec2
  "Functions to access the Amazon EC2 compute service.

  Each function takes a map of credentials as its first argument. The
  credentials map should contain an :access-key key and a :secret-key
  key. It can also contain an optional :endpoint key if you wish to
  specify an API endpoint (i.e. region) other than us-east."

  (:import com.amazonaws.AmazonServiceException
           com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.amazonaws.auth.BasicAWSCredentials
           com.amazonaws.services.ec2.AmazonEC2Client
           com.amazonaws.services.ec2.model.BlockDeviceMapping
           com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping
           com.amazonaws.services.ec2.model.EbsInstanceBlockDevice
           com.amazonaws.services.ec2.model.CreateImageRequest
           com.amazonaws.services.ec2.model.CreateTagsRequest
           com.amazonaws.services.ec2.model.DeleteTagsRequest
           com.amazonaws.services.ec2.model.DeregisterImageRequest
           com.amazonaws.services.ec2.model.DescribeImagesRequest
           com.amazonaws.services.ec2.model.DescribeInstancesRequest
           com.amazonaws.services.ec2.model.DescribeTagsRequest
           com.amazonaws.services.ec2.model.DescribeVolumesRequest
           com.amazonaws.services.ec2.model.EbsBlockDevice
           com.amazonaws.services.ec2.model.Filter
           com.amazonaws.services.ec2.model.GroupIdentifier
           com.amazonaws.services.ec2.model.IamInstanceProfileSpecification
           com.amazonaws.services.ec2.model.Image
           com.amazonaws.services.ec2.model.Instance
           com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification
           com.amazonaws.services.ec2.model.InstanceState
           com.amazonaws.services.ec2.model.InstanceStateChange
           com.amazonaws.services.ec2.model.Placement
           com.amazonaws.services.ec2.model.PrivateIpAddressSpecification
           com.amazonaws.services.ec2.model.ProductCode
           com.amazonaws.services.ec2.model.Reservation
           com.amazonaws.services.ec2.model.RunInstancesRequest
           com.amazonaws.services.ec2.model.StartInstancesRequest
           com.amazonaws.services.ec2.model.StopInstancesRequest
           com.amazonaws.services.ec2.model.TerminateInstancesRequest
           com.amazonaws.services.ec2.model.Tag
           com.amazonaws.services.ec2.model.TagDescription
           com.amazonaws.services.ec2.model.Volume
           com.amazonaws.services.ec2.model.VolumeAttachment)

  (:require [clojure.string :as string]))


(defn- ec2-client*
  "Create an AmazonEC2Client instance from a map of credentials."
  [{:keys [access-key secret-key endpoint]}]
  (let [credentials (if (and access-key secret-key)
                      (BasicAWSCredentials. access-key secret-key)
                      (DefaultAWSCredentialsProviderChain.))
        client (AmazonEC2Client. credentials)]
    (if endpoint (.setEndpoint client endpoint))
    client))

(def ^{:private true}
  ec2-client
  (memoize ec2-client*))


;;
;; convert object graphs to clojure maps
;;

(defprotocol ^{:no-doc true} Mappable
  "Convert a value into a Clojure map."
  (^{:no-doc true} to-map [x] "Return a map of the value."))

(extend-protocol Mappable nil (to-map [_] nil))


;;
;; convert clojure maps to object graphs

(defn- keyword-to-method
  "Convert a dashed keyword to a CamelCase method name"
  [kw]
  (apply str (map string/capitalize (string/split (name kw) #"-"))))

(defn set-fields
  "Use a map of params to call setters on a Java object"
  [obj params]
  (doseq [[k v] params]
    (let [method-name (str "set" (keyword-to-method k))
          method (first (clojure.lang.Reflector/getMethods (.getClass obj) 1 method-name false))
          arg-type (first (.getParameterTypes method))
          arg (if (= arg-type java.lang.Integer) (Integer. v) v)]
      (clojure.lang.Reflector/invokeInstanceMember method-name obj arg)))
  obj)

(declare mapper)

(defn map->ObjectGraph
  "Transform the map of params to a graph of AWS SDK objects"
  [params]
  (let [keys (keys params)]
    (zipmap keys (map #((mapper %) (params %)) keys))))

(defmacro mapper->
  "Creates a function that invokes set-fields on a new object of type
   with mapped parameters."
  [type]
  `(fn [~'params] (set-fields (new ~type) (map->ObjectGraph ~'params))))

;;
;; exceptions
;;

(extend-protocol Mappable
  AmazonServiceException
  (to-map [e]
    {:error-code   (.getErrorCode e)
     :error-type   (.name (.getErrorType e))
     :service-name (.getServiceName e)
     :status-code  (.getStatusCode e)
     :message      (.getMessage e)}))

(defn decode-exception
  "Returns a Clojure containing the details of an AmazonServiceException"
  [exception]
  (to-map exception))


;;
;; filters
;;

(defn aws-filter
  "Returns a Filter that can be used in calls to AWS to limit the results returned.

  E.g. (ec2/aws-filter \"tag:Name\" \"my-instance\")"
  [name & values]
  (Filter. name values))

(defn instance-filter
  "Returns a filter that can be used with ec2/describe-instances. It
  should be passed a Filter created by ec2/aws-filter."
  [& filters]
  (.withFilters (DescribeInstancesRequest.) filters))

(defn instance-id-filter
  "Returns an instance filter that can be passed to ec2/describe-instances to describe a single instance."
  [id]
  (instance-filter (aws-filter "instance-id" id)))

(defn image-filter
  "Returns a filter that can be used with ec2/describe-images. It
  should be passed a Filter created by ec2/aws-filter."
  [& filters]
  (.withFilters (DescribeImagesRequest.) filters))

(defn image-id-filter
  "Returns an image filter that can be passed to ec2/describe-images to describe a single image."
  [id]
  (image-filter (aws-filter "image-id" (str id))))

(defn image-owner-filter
  "Returns an image filter that can be passed to ec2/describe-images
  to describe a images owned by a user (e.g. \"self\" for the current
  user)."
  [owner]
  (.withOwners (DescribeImagesRequest.) [owner]))

(defn volume-filter
  "Returns a filter that can be used with ec2/describe-volumes. It
  should be passed a Filter created by ec2/aws-filter."
  [& filters]
  (.withFilters (DescribeVolumesRequest.) filters))

(defn volume-id-filter
  "Returns a volume filter that can be passed to ec2/describe-volumes to describe a single volume."
  [id]
  (volume-filter (aws-filter "volume-id" (str id))))

(defn volume-snapshot-filter
  "Returns a volume filter that can be passed to ec2/describe-volumes to describe volumes created from the specified snapshot."
  [snapshot-id]
  (volume-filter (aws-filter "snapshot-id" (str snapshot-id))))

(defn volume-status-filter
  "Returns a volume filter that can be passed to ec2/describe-volumes to describe volumes with the specified status (e.g. :in-use)."
  [status]
  (volume-filter (aws-filter "status" (name status))))

(defn tag-filter
  "Returns a filter that can be passed to ec2/describe-tags to limit the results returned. It
  should be passed a Filter created by ec2/aws-filter."
  [& filters]
     (DescribeTagsRequest. filters))

(defn tag-filter-by-resource-id
  "Returns a filter that can be passed to ec2/describe-tags to get all tags for a resource."
  [resource-id]
  (tag-filter (aws-filter "resource-id" resource-id)))

(defn tag-filter-by-resource-type
  "Returns a filter that can be passed to ec2/describe-tags to get all tags for a type of resource."
  [resource-type]
  (tag-filter (aws-filter "resource-type" resource-type)))


;;
;; tags
;;

(defn create-tag
  "Create an AWS Tag object from a key/value pair."
  [k v]
  ((mapper-> Tag) {:key (name k), :value v}))

(extend-protocol Mappable
  TagDescription
  (to-map [tag-description]
    {:key (.getKey tag-description)
     :value (.getValue tag-description)
     :resource-type (.getResourceType tag-description)
     :resource-id (.getResourceId tag-description)}))

(defn describe-tags
  "Describes one or more of the tags for your EC2 resources.

  You can specify filters to limit the response when describing tags.
  For example, you can use a filter to get only the tags for a
  specific resource. E.g.:

      (ec2/describe-tags cred (ec2/tag-filter-by-resource-id \"id-babecafe\"))

  You can also get all tags for a particular resource type:

      (ec2/describe-tags cred (ec2/tag-filter-by-resource-type \"image\"))

  Or specify your own filter:

      (ec2/describe-tags cred (ec2/tag-filter (ec2/aws-filter \"tag:Name\" \"*webserver*\")))

  See
  http://docs.amazonwebservices.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeTags.html#query-DescribeTags-filters
  for more information about supported filters."
  [cred filter]
  (map to-map (.getTags (.describeTags (ec2-client cred) filter))))

(defn create-tags
  "Adds or overwrites tags for the specified resources.

  Takes a list of resource ids (e.g. instance ids, AMI ids, etc.) and
  a map of tags to add or overwrite for the specified resources. E.g.:

      (ec2/create-tags cred [\"id-deadcafe\", \"ami-9465dbfd\"] {:name \"web server\" :owner \"ops\"})"
  [cred ids tags]
  (let [aws-tags (for [[k v] tags] (create-tag k v))]
    (.createTags (ec2-client cred) (CreateTagsRequest. ids aws-tags))))

(defn delete-tags
  "Deletes tags from the specified Amazon EC2 resources. E.g.:

  Takes a list of resource ids (e.g. instance ids, AMI ids, etc.) and
  a map of tags to delete from the specified resources. E.g.:

      (ec2/delete-tags cred [\"id-deadcafe\", \"ami-9465dbfd\"] {:name \"web server\" :owner \"ops\"})

  To delete a tag without regard to its current value, use `nil`:

      (ec2/delete-tags cred [\"id-deadcafe\"] {:owner nil})"
  [cred ids tags]
  (let [aws-tags (for [[k v] tags] (create-tag k v))]
    (.deleteTags (ec2-client cred) (.withTags (DeleteTagsRequest. ids) aws-tags))))


;;
;; instances
;;

(extend-protocol Mappable
  Tag
  (to-map [tag]
    {(.getKey tag) (.getValue tag)})

  InstanceState
  (to-map [instance-state]
    {:name (.getName instance-state)
     :code (.getCode instance-state)})

  InstanceStateChange
  (to-map [instance-state-change]
    {:id             (.getInstanceId instance-state-change)
     :current-state  (to-map (.getCurrentState instance-state-change))
     :previous-state (to-map (.getPreviousState instance-state-change))})

  EbsInstanceBlockDevice
  (to-map [ebs-instance-block-device]
    {:status      (.getStatus ebs-instance-block-device)
     :volume-id   (.getVolumeId ebs-instance-block-device)
     :attach-time (.getAttachTime ebs-instance-block-device)
     })

  InstanceBlockDeviceMapping
  (to-map [instance-block-device-mapping]
    {:device-name (.getDeviceName instance-block-device-mapping)
     :ebs         (to-map (.getEbs instance-block-device-mapping))
     })

  Placement
  (to-map [placement]
    {:availability-zone (.getAvailabilityZone placement)
     :group-name        (.getGroupName placement)
     :tenancy           (.getTenancy placement)})

  Instance
  (to-map [instance]
    {:id                    (.getInstanceId instance)
     :state                 (to-map (.getState instance))
     :type                  (.getInstanceType instance)
     :placement             (to-map (.getPlacement instance))
     :tags                  (reduce merge (map to-map (.getTags instance)))
     :image                 (.getImageId instance)
     :public-dns            (.getPublicDnsName instance)
     :private-ip-address    (.getPrivateIpAddress instance)
     :block-device-mappings (map to-map (.getBlockDeviceMappings instance))
     :launch-time           (.getLaunchTime instance)})

  GroupIdentifier
  (to-map [group-identifier]
    {:id   (.getGroupId group-identifier)
     :name (.getGroupName group-identifier)})

  Reservation
  (to-map [reservation]
    {:instances   (map to-map (.getInstances reservation))
     :group-names (flatten (.getGroupNames reservation))
     :groups      (map to-map (.getGroups reservation))}))

(defn describe-instances
  "List all the EC2 instances for the supplied credentials, applying the optional filter if supplied.

  Returns a list of Reservations, a data structure which contains the following keys:
    :instances   - a list of Instances
    :groups      - a list of security groups requested for the instances in this reservation
    :group-names - a list of security group names requested for the instances in this reservation

  You can specify filters to limit the number of instances returned or
  to find a specific instance. E.g.:

      (ec2/describe-instances cred (ec2/instance-id-filter \"i-deadcafe\"))"
  ([cred]
     (map to-map (.getReservations (.describeInstances (ec2-client cred)))))
  ([cred filter]
     (map to-map (.getReservations (.describeInstances (ec2-client cred) filter)))))

(defn start-instances
  "Start instance(s) that use Amazon EBS volumes as their root device.

  Any number of instance ids may be specified. E.g.:

      (ec2/start-instances cred \"i-beefcafe\" \"i-deadbabe\")

  Starting an already-running instance will have no effect."
  [cred & instance-ids]
  (map to-map (.getStartingInstances (.startInstances (ec2-client cred) (StartInstancesRequest. instance-ids)))))

(defn stop-instances
  "Stop instance(s) that use Amazon EBS volumes as their root device.

  Any number of instance ids may be specified. E.g.:

      (ec2/stop-instances cred \"i-beefcafe\" \"i-deadbabe\")

  Stopping an already-stopped instance will have no effect."
  [cred & instance-ids]
  (map to-map (.getStoppingInstances (.stopInstances (ec2-client cred) (StopInstancesRequest. instance-ids)))))

(defn- mapper
  "Most of the attributes of RunInstancesRequest are simple Java
   values (String, Integer, Boolean), but a few are composite types.
   This function returns a map of the attribute keys that contain
   composite values to functions that perform the appropriate
   transformation to concrete Java types from the AWS SDK."
  [key]
  (let [mappers
        {
         :iam-instance-profile  (fn [iam-instance-profile]  ((mapper-> IamInstanceProfileSpecification) iam-instance-profile))
         :placement             (fn [placement]             ((mapper-> Placement) placement))
         :ebs                   (fn [ebs]                   ((mapper-> EbsBlockDevice) ebs))
         ;; the following attributes contain vectors of maps
         :block-device-mappings (fn [block-device-mappings] (map (mapper-> BlockDeviceMapping) block-device-mappings))
         :network-interfaces    (fn [network-interfaces]    (map (mapper-> InstanceNetworkInterfaceSpecification) network-interfaces))
         :private-ip-addresses  (fn [private-ip-addresses]  (map (mapper-> PrivateIpAddressSpecification) private-ip-addresses))
    }]
    (if (contains? mappers key) (mappers key) identity)))

(defn run-instances
  "Launch EC2 instances.

  params is a map containing the parameters to send to AWS. E.g.:

  (ec2/run-instances cred { :min-count 1
                            :max-count 1
                            :image-id \"ami-9465dbfd\"
                            :instance-type \"t1.micro\"
                            :key-name \"my-key\" })

  There are many parameters available to control how instances are
  configured. E.g.:

  (ec2/run-instances cred { :min-count 1
                            :max-count 1
                            :image-id \"ami-9465dbfd\"
                            :instance-type \"t1.micro\"
                            :key-name \"my-key\"
                            :placement { :availability-zone \"ap-southeast-2\"}
                            :block-device-mappings [{:device-name  \"/dev/sdh\"
                                                     :ebs {:delete-on-termination false
                                                           :volume-size 120}}]
                            :network-interfaces [{:subnet-id \"subject-f00fbaaa\"
                                                  :device-index 0
                                                  :private-ip-addresses [{:private-ip-address \"10.1.1.103\"
                                                                          :primary true}]}]
                            })

  See
  http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/RunInstancesRequest.html
  for a complete list of available parameters.
  "
  [cred params]
  (to-map (.getReservation (.runInstances (ec2-client cred) ((mapper-> RunInstancesRequest) params)))))

(defn terminate-instances
  "Terminate instance(s).

  Any number of instance ids may be specified. E.g.:

      (ec2/terminate-instances cred \"i-beefcafe\" \"i-deadbabe\")"
  [cred & instance-ids]
  (map to-map (.getTerminatingInstances (.terminateInstances (ec2-client cred) (TerminateInstancesRequest. instance-ids)))))



;;
;; images
;;

(extend-protocol Mappable
  EbsBlockDevice
  (to-map [ebs-block-device]
    {:delete-on-termination (.getDeleteOnTermination ebs-block-device)
     :iops                  (.getIops ebs-block-device)
     :snapshot-id           (.getSnapshotId ebs-block-device)
     :volume-size           (.getVolumeSize ebs-block-device)
     :volume-type           (.getVolumeType ebs-block-device)})

  BlockDeviceMapping
  (to-map [block-device-mapping]
    {:device-name  (.getDeviceName block-device-mapping)
     :ebs          (to-map (.getEbs block-device-mapping))
     :no-device    (.getNoDevice block-device-mapping)
     :virtual-name (.getVirtualName block-device-mapping)})

  ProductCode
  (to-map [product-code]
    {:product-code-id   (.getProductCodeId product-code)
     :product-code-type (.getProductCodeType product-code)})

  Image
  (to-map [image]
    {:architecture          (.getArchitecture image)
     :block-device-mappings (map to-map (.getBlockDeviceMappings image))
     :description           (.getDescription image)
     :hypervisor            (.getHypervisor image)
     :image-id              (.getImageId image)
     :image-location        (.getImageLocation image)
     :image-owner-alias     (.getImageOwnerAlias image)
     :image-type            (.getImageType image)
     :kernel-id             (.getKernelId image)
     :name                  (.getName image)
     :owner-id              (.getOwnerId image)
     :platform              (.getPlatform image)
     :product-codes         (map to-map (.getProductCodes image))
     :public                (.getPublic image)
     :ramdisk-id            (.getRamdiskId image)
     :root-device-name      (.getRootDeviceName image)
     :root-device-type      (.getRootDeviceType image)
     :state                 (.getState image)
     :state-reason          (.getStateReason image)
     :tags                  (reduce merge (map to-map  (.getTags image)))
     :virtualization-type   (.getVirtualizationType image)}))

(defn describe-images
  "List all the EC2 images (AMIs), applying the optional filter if supplied.

  You can specify filters to limit the number of AMIs returned to
  those with a specific owner, or to find a specific AMI by id. E.g.:

      (ec2/describe-images cred (ec2/image-id-filter \"ami-9465dbfd\"))
      (ec2/describe-images cred (ec2/image-owner-filter \"self\"))"
  ([cred]
     (map to-map (.getImages (.describeImages (ec2-client cred)))))
  ([cred filter]
     (map to-map (.getImages (.describeImages (ec2-client cred) filter)))))

(defn create-image
  "Creates an Amazon EBS-backed AMI from a running or stopped instance and returns the
   new image's id.

  params is a map containing the instance id and name, and any other optional parameters.

  E.g.:

  (ec2/create-image cred { :instance-id \"i-deadbabe\"
                           :name \"web-snapshot\"
                           :description \"Snapshot of web server\"
                           :block-device-mappings [{:device-name  \"/dev/sdh\"
                                                    :ebs {:delete-on-termination false
                                                          :volume-size 120}}]})"
  [cred params]
  { :image-id (.getImageId (.createImage (ec2-client cred) ((mapper-> CreateImageRequest) params)))})

(defn deregister-image
  "Deregisters an AMI. Once deregistered, instances of the AMI can no longer be launched.

  E.g.:

  (ec2/deregister-image cred \"ami-9465dbfd\")"
  [cred image-id]
  (.deregisterImage (ec2-client cred) (DeregisterImageRequest. image-id)))


;;
;; volumes
;;

(extend-protocol Mappable
  Volume
  (to-map [volume]
    {:volume-id         (.getVolumeId volume)
     :volume-type       (.getVolumeType volume)
     :availability-zone (.getAvailabilityZone volume)
     :create-time       (.getCreateTime volume)
     :iops              (.getIops volume)
     :size              (.getSize volume)
     :snapshot-id       (.getSnapshotId volume)
     :state             (.getState volume)
     :tags              (reduce merge (map to-map (.getTags volume)))
     :attachments       (map to-map (.getAttachments volume))})

  VolumeAttachment
  (to-map [volume-attachment]
    {:volume-id             (.getVolumeId volume-attachment)
     :instance-id           (.getInstanceId volume-attachment)
     :device                (.getDevice volume-attachment)
     :state                 (.getState volume-attachment)
     :attach-time           (.getAttachTime volume-attachment)
     :delete-on-termination (.getDeleteOnTermination volume-attachment)}))

(defn describe-volumes
  "List EBS volumes, applying the optional filter if supplied.

  You can specify filters to limit the number of volumes returned to
  find a specific AMI by id, or with a given status. E.g.:

      (ec2/describe-volumes cred (ec2/volume-id-filter \"vol-98765432\"))
      (ec2/describe-volumes cred (ec2/volume-snapshot-filter \"snap-abcd1234\"))
      (ec2/describe-volumes cred (ec2/volume-status-filter :available)))
  See
  http://docs.aws.amazon.com/AWSEC2/latest/CommandLineReference/ApiReference-cmd-DescribeVolumes.html#cmd-DescribeVolumes-filters
  for a description of available filters."
  ([cred]
     (map to-map (.getVolumes (.describeVolumes (ec2-client cred)))))
  ([cred filter]
     (map to-map (.getVolumes (.describeVolumes (ec2-client cred) filter)))))
