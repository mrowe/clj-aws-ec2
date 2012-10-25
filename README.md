# clj-aws-ec2

A Clojure library for accessing Amazon EC2, based on the official AWS
Java SDK and borrowing heavily from James Reeves's [clj-aws-s3][]
library.

Currently the library supports functions to create, list, stop and start
EC2 instances, and to list AMI images.

[clj-aws-s3]: https://github.com/weavejester/clj-aws-s3

## Install

Add the following dependency to your `project.clj` file:

    [clj-aws-ec2 "0.1.1"]

## Example

```clojure
(require '[aws.sdk.ec2 :as ec2])

(def cred {:access-key "...", :secret-key "..."})

(ec2/describe-instances cred)
```

## Documentation

* [API docs](http://mrowe.github.com/clj-aws-ec2/)

## License

Copyright (C) 2012 Michael Rowe

Distributed under the Eclipse Public License, the same as Clojure.
