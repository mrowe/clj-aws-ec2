[![Build Status](https://buildhive.cloudbees.com/job/mrowe/job/clj-aws-ec2/badge/icon)](https://buildhive.cloudbees.com/job/mrowe/job/clj-aws-ec2/)
[![Build Status](https://travis-ci.org/mrowe/clj-aws-ec2.png)](https://travis-ci.org/mrowe/clj-aws-ec2)

# clj-aws-ec2

A Clojure library for accessing Amazon EC2, based on the official AWS
Java SDK and borrowing heavily from James Reeves's [clj-aws-s3][]
library.

This is a very early development version. Currently the library only
supports functions to list/describe EC2 reservations, instances and
machine images (AMIs), and to start and stop EBS-backed instances. See
[TODO][] for future plans.

[clj-aws-s3]: https://github.com/weavejester/clj-aws-s3
[TODO]: https://github.com/mrowe/clj-aws-ec2/wiki/TODO

## Install

Add the following dependency to your `project.clj` file:

    [clj-aws-ec2 "0.1.6"]

## Example

```clojure
(require '[aws.sdk.ec2 :as ec2])

(def cred {:access-key "...", :secret-key "..."})

(ec2/describe-instances cred)
(ec2/describe-instances cred (ec2/instance-id-filter "i-deadcafe"))

(ec2/describe-images cred (image-owner-filter "self"))
(ec2/describe-images cred (image-id-filter "ami-3c47a355"))

(ec2/start-instances cred "i-beefcafe")
(ec2/stop-instances cred "i-beefcafe" "i-deadbabe")


(ec2/run-instances cred { :min-count 1
                          :max-count 1
                          :image-id "ami-9465dbfd"
                          :instance-type "t1.micro"
                          :key-name "my-key" })
(ec2/terminate-instances cred "i-beefcafe" "i-deadbabe")
```

Refer to the
[API docs](http://mikerowecode.com/clj-aws-ec2/aws.sdk.ec2.html#var-run-instances)
for a more detailed example of the parameters available to
`run-instances`.

### Using regions

To use a region other than `us-east-1` you can specify an API endpoint
in the credentials map:

```clojure
(def cred {:access-key "...", :secret-key "...", :endpoint "ec2.ap-southeast-2.amazonaws.com"})
```

Refer to [Regions and Endpoints][] for a list of current EC2 endpoints.

[Regions and Endpoints]: http://docs.amazonwebservices.com/general/latest/gr/rande.html#ec2_region

### Exception handling

You can catch exceptions and extract details of the error condition:

```clojure
(try
  (ec2/start-instances cred "i-beefcafe")
  (catch Exception e (ec2/decode-exception e)))
```

`ec2/decode-exception` provides a map with the following keys:

    :error-code
    :error-type
    :service-name
    :status-code


## Documentation

* [API docs](http://mrowe.github.com/clj-aws-ec2/)

## History

### 0.1.6

 * Introduced run-instances and terminate-instances

### 0.1.5

 * Added support for regions

### 0.1.4

 * Introduced function to decode AWS exceptions

### 0.1.3

 * Introduced stop/start instances

### 0.1.2

 * Introduce describe-images

### 0.1.1

 * Initial release. Can only describe-instances.


## License

Copyright (C) 2012 Michael Rowe

Distributed under the Eclipse Public License, the same as Clojure.
