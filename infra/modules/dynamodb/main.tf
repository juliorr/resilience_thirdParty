resource "aws_dynamodb_table" "verification" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "verificationId"

  attribute {
    name = "verificationId"
    type = "S"
  }

  ttl {
    attribute_name = "expiresAt"
    enabled        = var.ttl_enabled
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = { Name = var.table_name }
}

data "aws_iam_policy_document" "access" {
  statement {
    sid    = "VerificationTableAccess"
    effect = "Allow"
    actions = [
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:DescribeTable",
    ]
    resources = [aws_dynamodb_table.verification.arn]
  }
}

resource "aws_iam_policy" "access" {
  name   = "${var.table_name}-access"
  policy = data.aws_iam_policy_document.access.json
}
