<#import "page.ftl" as page>
<@page.header/>

<h3>Modules</h3>

<div class="mt-4 mb-4">
<#list modules as _module>
    <div class="row">
        <div class="col-2">${_module.name}</div>
        <div class="col-2">${(_module.lastUpdate?string('dd.MM.yyyy HH:mm:ss'))!'--'}</div>
        <div class="col-2">${(_module.lastRead?string('dd.MM.yyyy HH:mm:ss'))!'--'}</div>
    </div>
</#list>
</div>

<p>
    <a class="btn btn-outline-primary" data-bs-toggle="collapse" href="#collapseExample" role="button" aria-expanded="false" aria-controls="collapseExample">
        Add another module
    </a>
</p>

<form method="post" class="needs-validation">
    <div class="collapse" id="collapseExample">
        <div class="card card-body">
            <div class="input-group mb-3" style="width:300px">
                <input type="text" class="form-control" name="m" placeholder="Module name" aria-label="New module name" aria-describedby="create-button" required>
                <button class="btn btn-outline-secondary" type="submit" id="create-button">Create</button>
            </div>
        </div>
    </div>
</form>

<!--<div class="input-group mb-3">-->
<!--    <input type="text" class="form-control" placeholder="Recipient's username" aria-label="Recipient's username" aria-describedby="button-addon2">-->
<!--    <button class="btn btn-outline-secondary" type="button" id="button-addon2">Button</button>-->
<!--</div>-->
<@page.jsFormValidation/>
<@page.footer/>