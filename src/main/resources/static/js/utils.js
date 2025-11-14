/**
 * 是否 数字类型
 *
 * @param str
 * @returns {boolean}
 */
function isNumeric(str) {
    return Number.isFinite(Number(str));
}

/**
 * 转换为数字
 *
 * @param value
 * @returns {number}
 */
function toNumber(value) {
    return Number(value);
}


/**
 * 是否 布尔值类型
 *
 * @param strOrBool
 * @returns {boolean}
 */
function isBoolean(strOrBool) {
    try {
        return toBoolean(strOrBool);
    } catch (e) {
        return false;
    }
}

/**
 * 转换为布尔值
 *
 * @param strOrBool
 * @returns {boolean}
 */
function toBoolean(strOrBool) {
    if (strOrBool === true || strOrBool === 'true') {
        return true;
    } else if (strOrBool === false || strOrBool === 'false') {
        return false;
    }

    // 例如，如果输入不是 "true" 或 "false"，则返回 false 或 null
    // return false;
    throw new Error("Invalid boolean string : " + strOrBool);
}