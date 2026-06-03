resource "aws_ssm_parameter" "table_name" {
  name  = "/${var.name}/verification-table-name"
  type  = "String"
  value = var.table_name
}

resource "aws_ssm_parameter" "region" {
  name  = "/${var.name}/aws-region"
  type  = "String"
  value = var.region
}
