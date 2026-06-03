output "table_name_parameter" {
  value = aws_ssm_parameter.table_name.name
}

output "region_parameter" {
  value = aws_ssm_parameter.region.name
}
