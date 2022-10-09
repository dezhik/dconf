<#import "page.ftl" as page>
<@page.header/>


<#if emptyResults()>
    <span>No clients have connected yet.</span>
<#else>
    <div class="mt-4 mb-4">
        <div class="row mb-2">
            <div class="col-2"><b>Host</b></div>
            <div class="col-2"><b>Version</b></div>
            <div class="col-2"><b>Last sync</b></div>
            <div class="col-2"><b>Modules</b></div>
        </div>
    <#list clients as _client>
        <div class="row">
            <div class="col-2">${_client.host}</div>
            <div class="col-2">${_client.version}</div>
            <div class="col-2">${(_client.lastRead?string('dd.MM.yyyy HH:mm:ss'))!'--'}</div>
            <div class="col-2">${_client.modulesList!''}</div>
        </div>
    </#list>

    </div>
</#if>



<!--<div class="input-group mb-3">-->
<!--    <input type="text" class="form-control" placeholder="Recipient's username" aria-label="Recipient's username" aria-describedby="button-addon2">-->
<!--    <button class="btn btn-outline-secondary" type="button" id="button-addon2">Button</button>-->
<!--</div>-->

<@page.footer/>