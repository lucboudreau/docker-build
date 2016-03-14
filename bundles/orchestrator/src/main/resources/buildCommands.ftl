Build Commands:
==============
Build:
------
<#list build.commands as command>
${command?index + 1}. ${command}
</#list>
<#if build.cleanupCommand??>

Cleanup:
-------
${build.cleanupCommand}
</#if>
