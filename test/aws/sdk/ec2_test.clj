(ns aws.sdk.ec2-test
  (:use clojure.test
        aws.sdk.ec2))


;;
;; filters
;;

(let [filter (aws-filter "foo" "bar")]

  (deftest filter-is-a-vector
    (testing "AWS filter provides a vector"
      (is (vector? filter))))

  (deftest filter-sets-name-and-one-value
    (testing "AWS filter"
      (let [f (first filter)]
        (is (= (.getName f) "foo"))
        (is (= (.getValues f) '("bar"))))))

  (deftest filter-sets-two-values
    (testing "AWS filter"
      (let [filter (first (aws-filter "foo" "bar" "baz"))]
        (is (= (.getValues filter) '("bar" "baz")))))))


(let [filter (image-id-filter "foo")]

  (deftest image-id-filter-is-a-describe-images-request
    (testing "Image id filter is a DescribeImagesRequest"
      (is (= (.getClass filter) com.amazonaws.services.ec2.model.DescribeImagesRequest))))

  (let [f (first (.getFilters filter))]
  
    (deftest image-id-filter-sets-image-id-filter
      (testing "Image id filter uses image-id filter"
        (is (= (.getName f) "image-id"))))
    
    (deftest image-id-filter-sets-image-id
      (testing "Image id filter uses given image id"
        (is (= (first (.getValues f)) "foo"))))))


(let [filter (image-owner-filter "foo")]

  (deftest image-owner-filter-is-a-describe-images-request
    (testing "Image owner filter is a DescribeImagesRequest"
      (is (= (.getClass filter) com.amazonaws.services.ec2.model.DescribeImagesRequest))))

  (deftest image-own-filter-sets-owner
    (testing "Image owner filter sets a given owner"
      (is (= (first (.getOwners filter)) "foo")))))

;;
;; clojure -> object graph mapping
;;

(deftest simple-run-instances-request
  (testing "Can create a simple run-instances request"
    (let [r (->RunInstancesRequest {:min-count 1, :max-count 2, :image-id "ami-9465dbfd" })]
      (is (= (.getMinCount r) 1))
      (is (= (.getMaxCount r) 2))
      (is (= (.getImageId r) "ami-9465dbfd")))))

(deftest run-instances-request-with-placement
  (testing "Can create a run-instances request with placement info"
    (let [r (->RunInstancesRequest {:placement { :availability-zone "az" }})]
      (is (= (.. r (getPlacement) (getAvailabilityZone)) "az")))))

(deftest run-instances-request-with-block-device-mappings
  (let [block-device-mapping {:device-name  "/dev/sdh",
                              :ebs {:delete-on-termination false,
                                    :volume-size 120}}]

    (testing "Can create a run-instances request with no block device mappings"
      (let [r (->RunInstancesRequest { })]
        (is (= (.. r (getBlockDeviceMappings) (size)) 0))))
    
    (testing "Can create a run-instances request with one block device mapping"
      (let [r (->RunInstancesRequest { :block-device-mappings [block-device-mapping] })]
        (is (= (.. r (getBlockDeviceMappings) (size)) 1))
        (let [device-mapping (.. r (getBlockDeviceMappings) (get 0))]
          (is (= (.getDeviceName device-mapping)) "/dev/sdh")
          (is (false? (.. device-mapping (getEbs) (getDeleteOnTermination))))
          (is (= (.. device-mapping (getEbs) (getVolumeSize)) 120)))))

    (testing "Can create a run-instances request with two block device mappings"
      (let [r (->RunInstancesRequest { :block-device-mappings [block-device-mapping, block-device-mapping] })]
        (is (= (.. r (getBlockDeviceMappings) (size)) 2))))))

(deftest run-instances-request-with-network-interfaces

    (testing "Can create a run-instances request with no network interfaces"
      (let [r (->RunInstancesRequest { })]
        (is (= (.. r (getNetworkInterfaces) (size)) 0))))

    (testing "Can create a run-instances request with a network interface with a single ip"
      (let [r (->RunInstancesRequest { :network-interfaces [{:subnet-id  "abcdef",
                                                             :network-interface-id "eth1",
                                                             :private-ip-address "10.1.1.1"}]})]
        (is (= (.. r (getNetworkInterfaces) (size)) 1))
        (let [network-interfaces (.. r (getNetworkInterfaces) (get 0))]
          (is (= (.getSubnetId network-interfaces) "abcdef"))
          (is (= (.getNetworkInterfaceId network-interfaces) "eth1"))
          (is (= (.getPrivateIpAddress network-interfaces) "10.1.1.1")))))


    (testing "Can create a run-instances request with a network interface with two ips"
      (let [r (->RunInstancesRequest { :network-interfaces [{:subnet-id  "abcdef",
                                                             :network-interface-id "eth1",
                                                             :private-ip-addresses [ {:private-ip-address "10.1.1.1",
                                                                                      :primary true},
                                                                                     {:private-ip-address "10.1.2.2",
                                                                                      :primary false}]}]})]
        (is (= (.. r (getNetworkInterfaces) (size)) 1))
        (let [network-interfaces (.. r (getNetworkInterfaces) (get 0))]
          (is (= (.getSubnetId network-interfaces) "abcdef"))
          (is (= (.getNetworkInterfaceId network-interfaces) "eth1"))
          (let [addresses (.getPrivateIpAddresses network-interfaces)]
            (is (= (.size addresses) 2))
            (let [address (.get addresses 0)]
              (is (= (.getPrivateIpAddress address) "10.1.1.1"))
              (is (true? (.isPrimary address))))
            (let [address (.get addresses 1)]
              (is (= (.getPrivateIpAddress address) "10.1.2.2"))
              (is (false? (.isPrimary address)))))))))
