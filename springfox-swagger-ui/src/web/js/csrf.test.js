import 'babel-polyfill';
import patchRequestInterceptor, { getCsrf, csrfRequestInterceptor } from './csrf';
import fetchMock from 'fetch-mock';
import { FetchError } from 'node-fetch';

const baseUrl = 'http://mock.com';
const headerName = 'X-XSRF-TOKEN';
const cookieName = 'XSRF-TOKEN';
const token = 'b11d3ee4-51d4-4eda-9980-6c07e527eb44';

afterEach(() => {
  fetchMock.reset();
  fetchMock.restore();
  // clear cookie
  document.cookie
    .split(";")
    .forEach((c) => {
      document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
    });
});

async function expectOk() {
  const response = await getCsrf(baseUrl);
  expect(response.token).toBe(token);
  expect(response.headerName).toBe(headerName);
}

/*
 * 1 2 3: get from cookie, get from endpoint, get from meta
 * x: mock csrf not found
 * o: mock found!
 * ?: mock not define
 */

test('x x x', async () => {
  document.cookie = 'first=hi';
  document.cookie = 'last=hi';
  fetchMock.mock(`${baseUrl}/`, 404);
  fetchMock.mock(`${baseUrl}/csrf`, 404);

  const response = await getCsrf(baseUrl);
  expect(response).toBe(undefined);
});

test('o ? ?', async () => {
  document.cookie = 'first=hi';
  document.cookie = `${cookieName}=${token}`;
  document.cookie = 'last=hi';

  await expectOk();
});

test('x o ?', async () => {
  document.cookie = 'first=hi';
  document.cookie = 'last=hi';
  fetchMock.mock(`${baseUrl}/csrf`, { headerName, token });

  await expectOk();
});

test('x x o', async () => {
  document.cookie = 'first=hi';
  document.cookie = 'last=hi';
  fetchMock.mock(`${baseUrl}/csrf`, 404);
  fetchMock.mock(`${baseUrl}/`, `<html><head><title>Title</title><meta name="_csrf" content="${token}"><meta name="_csrf_header" content="${headerName}"></head><body></body></html>`);
  await expectOk();
});

test('x wrong-json ?', async () => {
  fetchMock.mock(`${baseUrl}/`, 404);
  fetchMock.mock(`${baseUrl}/csrf`, {
    nothing: 'nothing'
  });

  const response = await getCsrf(baseUrl);
  expect(response).toBe(undefined);
});

test('x invalid-json ?', async () => {
  fetchMock.mock(`${baseUrl}/`, 404);
  fetchMock.mock(`${baseUrl}/csrf`, 'bla bla bla');

  let error;
  try {
    await getCsrf(baseUrl)
  } catch (e) {
    error = e;
  }
  expect(error).toBeInstanceOf(FetchError);
});


test('interceptor initialization', async () => {
  window.ui = {};
  window.ui.getConfigs = () => { return {} };


  window.ui = {};
  window.ui.getConfigs = () => { return {} };

  // Make sure this function will not throw exception.
  await patchRequestInterceptor(baseUrl);
});

test('request interceptor chain', async () => {
  fetchMock.mock(`${baseUrl}/`, 404);
  fetchMock.mock(`${baseUrl}/csrf`, 404);

  var updatedByCustomInterceptor = false;
  var customInterceptor = request => {
    updatedByCustomInterceptor = true;
    return request;
  };

  window.ui = {};
  window.ui.getConfigs = () => { return { requestInterceptor: customInterceptor } };

  // Initialize interceptor
  await patchRequestInterceptor(baseUrl);

  // Initialization should not invoke interceptor
  expect(updatedByCustomInterceptor).toBeFalsy();

  // Invoke interceptor chain
  window.ui.getConfigs().requestInterceptor({ url: baseUrl });

  // Custom interceptor must have been called
  expect(updatedByCustomInterceptor).toBeTruthy();
});

test('cross-origin-request', async () => {
  var request = { url: `${baseUrl}/test-endpoint`, headers: [] };
  const csrf = {
    headerName: `${headerName}`,
    token: `${token}`
  };
  const modifiedRequest = csrfRequestInterceptor(request, csrf);

  expect(modifiedRequest).toBe(request);
  expect(modifiedRequest.headers).toHaveLength(0);
});

test('same-origin-request', async () => {
  const request = { url: `http://localhost/test-endpoint`, headers: [] };
  const csrf = {
    headerName: `${headerName}`,
    token: `${token}`
  };

  const modifiedRequest = csrfRequestInterceptor(request, csrf);

  expect(modifiedRequest).toBe(request);
  expect(modifiedRequest.headers[headerName]).toBe(token);
});
