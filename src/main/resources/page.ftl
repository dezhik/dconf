<#macro header>
<html>
<head>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-ka7Sk0Gln4gmtz2MlQnikT1wXgYsOg+OMhuP+IlRH9sENBO0LRn5q+8nbTov4+1p" crossorigin="anonymous"></script>
    <link href = "https://fonts.googleapis.com/icon?family=Material+Icons" rel = "stylesheet">

</head>
<body>
<style>
    .material-icons.md-18 { font-size: 18px; }
    .material-icons.md-24 { font-size: 24px; }
    .material-icons.md-36 { font-size: 36px; }
    .material-icons.md-48 { font-size: 48px; }
    .material-icons.md-dark { color: rgba(0, 0, 0, 0.54); }
    .material-icons.md-dark.md-inactive { color: rgba(0, 0, 0, 0.26); }
    .input-group-sm { width:250px; }
</style>
<div class="card">
    <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
            <li class="nav-item">
                <a class="nav-link ${(currentPage! == 'index')?then('active','')}" aria-current="true" href="/">Index</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${(currentPage! == 'modules')?then('active','')}" href="/modules">Modules</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${(currentPage! == 'clients')?then('active','')}" href="/clients">Clients</a>
            </li>
            <li class="nav-item p-1 ps-4">
                <form action="/search" style="margin-block-end:0px;padding-left:200px">
                    <div class="d-flex ">
<!--                        flex-row-->

<!--                    <div class="col col-md-auto">-->
                    <div class="">
                        <div class="input-group input-group-sm">
                            <label class="input-group-text" for="moduleSelect">Module</label>
                            <select name="m" class="form-select" id="moduleSelect">
                                <option>All modules</option>
                                <#list modules as _module>
                                    <option value="${_module.name}" ${(selectedModule?? && _module.name == selectedModule)?then('selected','')}>
                                        ${_module.name}
                                    </option>
                                </#list>
                            </select>
                        </div>
                    </div>
<!--                    <div class="col col-md-auto">-->
<!--                        <div class="input-group input-group-sm ps-1">-->
<!--                            <span class="input-group-text" id="hostDesc">Host</span>-->
<!--                            <input type="text" name="h" class="form-control" value="${hostValue!''}" aria-label="Host" aria-describedby="hostDesc">-->
<!--                        </div>-->
<!--                    </div>-->


<!--                    <div class="col col-md-auto">-->
                    <div class="">
                        <div class="input-group input-group-sm ps-1">
                            <span class="input-group-text" id="propDesc">Property</span>
                            <input type="text" name="sn" class="form-control" value="${propertySearchQuery!''}" aria-label="Property" aria-describedby="propDesc">
                        </div>
                    </div>
<!--                    <div class="col col-md-auto">-->
                    <div class="">
                        <div class="input-group input-group-sm ps-2">
                            <button type="submit" class="btn btn-outline-secondary">Search</button>
                        </div>
                    </div>
                    <!--        <div class="col">-->
                    <!--            <div class="input-group input-group-sm ps-1">-->
                    <!--                <span class="input-group-text" id="valueDesc">Value</span>-->
                    <!--                <input type="text" name="v" class="form-control" aria-label="Sizing example input" aria-describedby="valueDesc">-->
                    <!--            </div>-->
                    <!--        </div>-->
<!--                    <div class="col col-md-3 ms-auto">-->
                    <div class="">
                        <a href="/new" class="ps-4 me-1 position-absolute end-0"><!--   -->
                            <button type="button" class="btn btn-outline-success btn-sm">
                                <span class="material-icons md-dark md-18 align-middle">add</span>
                                <span class="align-middle">
                                    New property
                                </span>
                            </button>
                        </a>
                    </div>
                    </div>
                </form>
            </li>
        </ul>
    </div>

    <div class="card-body">
</#macro>

<#macro footer>
        </div>
    </div>
    <div class="d-flex flex-row-reverse">
        <div class="p-2">v. ${appVersion}</div>
        <div class="p-2">Started at ${appStartTime?string('d MMM HH:mm:ss')!''}</div>
    </div>

</body>
</html>
</#macro>

<#macro decoratedList properties>
<table class="table table-striped">
    <thead class="thead-dark">
    <tr>
        <th scope="col">Module</th>
        <th scope="col">Host</th>
        <th scope="col">Name</th>
        <th scope="col">Value</th>
        <th scope="col"></th>
    </tr>
    </thead>
    <tbody>
        <#list properties as prop>
        <tr >
            <th scope="row">${prop.module}</th>
            <td>
                <#if (prop.defaultHost)>
                default ${(prop.counter > 0)?then('(+' + prop.counter + ' other hosts)','')}
                <#else>
                (+ ${prop.counter!'--'} other hosts)
            </#if>
        </td>
        <td>${prop.name}</td>
        <td>${(prop.host == 'default')?then(prop.shortcut,'')}</td>
        <td><a href="/search?m=${prop.module}&n=${prop.name}">View</a></td>
        <td>
            <a href="/edit?m=${prop.module}&h=${prop.host}&n=${prop.name}"><span class="material-icons md-dark md-18">edit</span></a>
            <span class="material-icons md-dark md-18 ml-4">delete</span>
            <span>(v. ${prop.version})</span>
        </td>
        </tr>
        </#list>
    </tbody>
</table>
</#macro>

<#macro jsFormValidation>
<script>
(function () {
  'use strict'
  var forms = document.querySelectorAll('.needs-validation')
  Array.prototype.slice.call(forms)
    .forEach(function (form) {
      form.addEventListener('submit', function (event) {
        if (!form.checkValidity()) {
          event.preventDefault()
          event.stopPropagation()
        }

        form.classList.add('was-validated')
      }, false)
    })
})();
</script>
</#macro>