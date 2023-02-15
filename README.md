# Playstation Project

This is a project template for AEM-based applications. 
It creates a servlet to get the list of page having a particular component with number of times the component being used on a page.

Input values to the servlet:
1) Root path - Search root path

2) Component path - The path of the component to be searched. Eg: playstation/components/title

3) formatOutput - Optional param to display formatted output.


## Modules

The main parts of the template are:

* core: Java bundle containing component-related Java code such as servlets and services.
* ui.apps: contains the /apps parts of the project. Uses maven archetype default.
* ui.content: contains test content at /content/playstation built using the components from the ui.apps
* ui.config: contains OSGi configs for creating & mapping a system user to core bundle.
* all: a single content package that embeds all of the compiled modules (bundles and content packages)


## Outputs
`$ curl -u admin:admin 'http://localhost:4502/content/playstation/us/en/list-pages.csearch.html?searchRoot=/content/playstation&componentPath=playstation/components/helloworld'
/content/playstation/ca/en=1
/content/playstation/ca/en/page1=2
/content/playstation/ca/en/page2=2
/content/playstation/ca/en/page3=2
/content/playstation/ca/en/page4=1
/content/playstation/us/en=1
/content/playstation/us/en/page1=2
/content/playstation/us/en/page2=1
/content/playstation/us/en/page3=2
/content/playstation/us/en/page4=2
/content/playstation/us/en/page5=1
/content/playstation/us/en/page6=1`

`$ curl -u admin:admin  'http://localhost:4502/content/playstation/us/en/list-pages.csearch.html?searchRoot=/content/playstation&componentPath=playstation/components/text'
/content/playstation/ca/en/page1=1
/content/playstation/ca/en/page2=1
/content/playstation/ca/en/page4=1
/content/playstation/us/en/page1=3
/content/playstation/us/en/page2=1
/content/playstation/us/en/page3=1
/content/playstation/us/en/page4=1
/content/playstation/us/en/page7=2`

Alternatively, the search can be performed from an html page:

http://localhost:4502/content/playstation/us/en/list-pages.csearch.html?searchRoot=/content/playstation&componentPath=playstation/components/title&formatOutput=true


## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage
