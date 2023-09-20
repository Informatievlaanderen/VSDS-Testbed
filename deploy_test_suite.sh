echo Deploying test suite...
rm -f test_suite.zip
7z a test_suite.zip .\test_suite\*
curl -F updateSpecification=true -F specification=82D0F3BFXF786X449BXB36DX298C0666E45C -F testSuite=@test_suite_must.zip --header "ITB_API_KEY: D1D33F62X51DBX49BFXAFB4X7E2690F8822F" -X POST http://localhost:9000/api/rest/testsuite/deploy;
curl -F updateSpecification=true -F specification=82D0F3BFXF786X449BXB36DX298C0666E45C -F testSuite=@test_suite_should.zip --header "ITB_API_KEY: D1D33F62X51DBX49BFXAFB4X7E2690F8822F" -X POST http://localhost:9000/api/rest/testsuite/deploy;
curl -F updateSpecification=true -F specification=82D0F3BFXF786X449BXB36DX298C0666E45C -F testSuite=@test_suite_optional.zip --header "ITB_API_KEY: D1D33F62X51DBX49BFXAFB4X7E2690F8822F" -X POST http://localhost:9000/api/rest/testsuite/deploy;
