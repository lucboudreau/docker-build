<#if 0 < violations?size >
License header violations:
==========================
<#list violations as violation>
${violation?index + 1}. ${violation}
</#list>
</#if>
