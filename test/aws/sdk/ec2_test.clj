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
