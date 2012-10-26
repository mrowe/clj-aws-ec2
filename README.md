[![Build Status](https://travis-ci.org/mrowe/clj-aws-ec2.png)](https://travis-ci.org/mrowe/clj-aws-ec2)

# clj-aws-ec2

A Clojure library for accessing Amazon EC2, based on the official AWS
Java SDK and borrowing heavily from James Reeves's [clj-aws-s3][]
library.

This is a very early development version. Currently the library only
supports functions to list/describe EC2 reservations, instances and
machine images (AMIs). See [TODO][] for future plans.

[clj-aws-s3]: https://github.com/weavejester/clj-aws-s3
[TODO]: TODO.md

## Install

Add the following dependency to your `project.clj` file:

    [clj-aws-ec2 "0.1.3"]

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
```

## Documentation

* [API docs](http://mrowe.github.com/clj-aws-ec2/)

## License

Copyright (C) 2012 Michael Rowe

Distributed under the Eclipse Public License, the same as Clojure.
