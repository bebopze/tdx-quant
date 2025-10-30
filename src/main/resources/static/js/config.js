// static/js/config.js
(function (window) {
    'use strict';

    // 环境配置
    const ENV_CONFIG = {
        dev: {
            API_HOST: 'localhost:7001',
            DEBUG: true
        },
        test: {
            API_HOST: 'localhost:7001',
            DEBUG: true
        },
        prod: {
            API_HOST: 'localhost:7001',
            DEBUG: false
        }
    };

    // 获取环境 - 支持通过 URL 参数强制指定环境
    function getEnvironment() {
        // 检查 URL 参数
        const urlParams = new URLSearchParams(window.location.search);
        const envParam = urlParams.get('env');
        if (envParam && ENV_CONFIG[envParam]) {
            return envParam;
        }

        // 根据域名判断
        const hostname = window.location.hostname;
        if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname.startsWith('192.168.') || hostname.includes('dev')) {
            return 'dev';
        } else if (hostname.includes('test') || hostname.includes('staging')) {
            return 'test';
        } else {
            return 'prod';
        }
    }

    // 获取当前环境配置
    const currentEnv = getEnvironment();
    const config = ENV_CONFIG[currentEnv];

    // 创建 API 工具类
    const ApiUtils = {
        getConfig: () => config,
        getEnv: () => currentEnv,
        API_HOST: config.API_HOST,

        buildUrl: (path) => {
            const baseUrl = 'http://' + config.API_HOST;
            const cleanPath = path.startsWith('/') ? path : '/' + path;
            return baseUrl + cleanPath;
        },

        // 封装 fetch 方法
        fetch: (path, options = {}) => {
            const url = ApiUtils.buildUrl(path);
            return fetch(url, options);
        }
    };

    // 挂载到全局
    window.APP_CONFIG = config;
    window.ApiUtils = ApiUtils;

    if (config.DEBUG) {
        console.log('API 配置:', {
            environment: currentEnv,
            apiHost: config.API_HOST,
            debug: config.DEBUG
        });
    }

})(window);