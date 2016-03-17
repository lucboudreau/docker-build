<#if 0 < sensitiveFiles?size >
Build Sensitive Files Changed:
==============================
<#list sensitiveFiles as file >
  ${file}
</#list>
</#if>