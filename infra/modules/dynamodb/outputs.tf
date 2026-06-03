output "table_name" {
  value = aws_dynamodb_table.verification.name
}

output "table_arn" {
  value = aws_dynamodb_table.verification.arn
}

output "access_policy_arn" {
  value = aws_iam_policy.access.arn
}
