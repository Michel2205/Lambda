####################
S3BucketEncryptionValidatorService 
####################

This repository contains AWS Lambda function with Java AWS SDK 

About this Lambda function
==================

This function evaluate all buckets on especific rol asigneed. Its function is get all policies,
from each bucket, and recognize if has two specific policies, DenyIncorrectEncryptionHeader and 
DenyUnEncryptedObjectUploads

How it work
-----------------------------

If some bucket has not policies are add both policies.
If a bucket has some policy or policies and has not this policies, merge its policies with ours.
In case of a bucket is policy missing must be add the missin one.