<#import "page.ftl" as page>
<@page.header/>

<#if edit>
    <h3>Edit property</h3>
<#else>
    <h3>Add new property</h3>
</#if>


<#if error??>
<div class="alert alert-danger" role="alert">
    ${error}
</div>
</#if>

<form class="needs-validation" method="post" novalidate>
    <div class="row g-3 align-items-center">
        <div class="col-1">
            <label for="propName" class="col-form-label">Module:</label>
        </div>
        <div class="col-auto">
            <select name="pModule" class="form-select" aria-label="Select module" ${edit?then('disabled','')} style="width:200px;">
                <#list modules as _module>
                <option value="${_module.name}" ${(module?? && _module.name == module)?then('selected','')}>${_module.name}</option>
                </#list>
            </select>
        </div>
    </div>

    <div class="row g-3 mt-1 align-items-center">
        <div class="col-1">
            <label for="propHost" class="col-form-label">Host:</label>
        </div>
        <div class="col-auto">
            <input name="pHost" id="propHost" class="form-control" value="${host!''}" aria-describedby="propHostHelpInline" ${edit?then('disabled','')}/>
<!--            <span class="material-icons outlined">help_outline</span>-->
        </div>
        <div class="col-auto">
            <span id="propHostHelpInline" class="form-text">
              Select a specific host or leave common-host.
            </span>
        </div>
    </div>

    <div class="row g-3 mt-1 align-items-center">
        <div class="col-1">
            <label for="propName" class="col-form-label">Name:</label>
        </div>
        <div class="col-auto">
            <input name="pVersion" type="hidden" value="${version!''}" />
            <input name="pName" id="propName" class="form-control" aria-describedby="propNameHelpInline" value="${name!''}" ${edit?then('disabled','')} required />
        </div>
        <div class="col-auto">
            <span id="propNameHelpInline" class="form-text">
              Should be unique within a module.
            </span>
        </div>
    </div>

    <div class="mb-3 mt-4">
        <textarea name="pValue" class="form-control" id="propValue" rows="6" placeholder="Property value. May be left empty">${value!''}</textarea>
    </div>

    <div class="col-12">
        <button class="btn btn-primary" type="submit">
            ${edit?then('Edit','Add')}
        </button>
    </div>
</form>

<@page.jsFormValidation/>

<@page.footer/>