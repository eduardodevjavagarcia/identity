#!/bin/bash
set -euo pipefail

awslocal sqs create-queue \
  --queue-name identity-events.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

awslocal sqs create-queue \
  --queue-name identity-compliance-acl.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

TOPIC_ARN=$(awslocal sns create-topic \
  --name compliance-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=false \
  --query 'TopicArn' --output text)

QUEUE_URL=$(awslocal sqs get-queue-url \
  --queue-name identity-compliance-acl.fifo \
  --query 'QueueUrl' --output text)

QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' --output text)

awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --attributes RawMessageDelivery=true

awslocal s3 mb s3://identity-retention
