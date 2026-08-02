"use weex:vue";

if (typeof Promise !== 'undefined' && !Promise.prototype.finally) {
  Promise.prototype.finally = function(callback) {
    const promise = this.constructor
    return this.then(
      value => promise.resolve(callback()).then(() => value),
      reason => promise.resolve(callback()).then(() => {
        throw reason
      })
    )
  }
};

if (typeof uni !== 'undefined' && uni && uni.requireGlobal) {
  const global = uni.requireGlobal()
  ArrayBuffer = global.ArrayBuffer
  Int8Array = global.Int8Array
  Uint8Array = global.Uint8Array
  Uint8ClampedArray = global.Uint8ClampedArray
  Int16Array = global.Int16Array
  Uint16Array = global.Uint16Array
  Int32Array = global.Int32Array
  Uint32Array = global.Uint32Array
  Float32Array = global.Float32Array
  Float64Array = global.Float64Array
  BigInt64Array = global.BigInt64Array
  BigUint64Array = global.BigUint64Array
};


(()=>{var i=Object.create;var l=Object.defineProperty;var f=Object.getOwnPropertyDescriptor;var m=Object.getOwnPropertyNames;var b=Object.getPrototypeOf,g=Object.prototype.hasOwnProperty;var y=(e,t)=>()=>(t||e((t={exports:{}}).exports,t),t.exports);var w=(e,t,r,s)=>{if(t&&typeof t=="object"||typeof t=="function")for(let o of m(t))!g.call(e,o)&&o!==r&&l(e,o,{get:()=>t[o],enumerable:!(s=f(t,o))||s.enumerable});return e};var v=(e,t,r)=>(r=e!=null?i(b(e)):{},w(t||!e||!e.__esModule?l(r,"default",{value:e,enumerable:!0}):r,e));var n=y((I,_)=>{_.exports=Vue});var c=v(n());var u=(e,t)=>{let r=e.__vccOpts||e;for(let[s,o]of t)r[s]=o;return r};var d={};function x(e,t){return(0,c.openBlock)(),(0,c.createElementBlock)("scroll-view",{scrollY:!0,showScrollbar:!0,enableBackToTop:!0,bubble:"true",style:{flexDirection:"column"}},[(0,c.createElementVNode)("view")])}var a=u(d,[["render",x]]);var p=plus.webview.currentWebview();if(p){let e=parseInt(p.id),t="pages/tabBar/API/API",r={};try{r=JSON.parse(p.__query__)}catch(o){}a.mpType="page";let s=Vue.createPageApp(a,{$store:getApp({allowDefault:!0}).$store,__pageId:e,__pagePath:t,__pageQuery:r});s.provide("__globalStyles",Vue.useCssStyles([...__uniConfig.styles,...a.styles||[]])),s.mount("#root")}})();
