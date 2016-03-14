<#if 0 < violations?size >
Checkstyle violations:
======================
See: https://github.com/pentaho/pentaho-coding-standards
<#list violations as violation >
File ${violation.fileName}
<#list violation.checkstyleErrors as error >
${error?index + 1}. Message: ${error.message}<#if error.line?? > at Line: ${ error.line }<#if error.column?? > Column: ${ error.column }</#if></#if>
</#list>
</#list>
</#if>
