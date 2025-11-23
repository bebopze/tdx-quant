/**
 * 是否 数字类型
 *
 * @param str
 * @returns {boolean}
 */
function isNumber(str) {
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


/**
 * 通用排序（通过 指定列字段名 和 排序顺序）
 *
 * @param sortedList 要排序的列表
 * @param sortState  排序状态对象，包含 column（排序列字段名）和 order（排序顺序：asc/desc）      e.g：{column: 'mktval', order: 'desc'};
 */
function sort(sortedList, sortState) {
    sortedList.sort((a, b) => {
        let valA = a[sortState.column];
        let valB = b[sortState.column];
        if (valA == null) valA = '';
        if (valB == null) valB = '';

        // number
        if (typeof valA === 'number') {
            return sortState.order === 'asc' ? valA - valB : valB - valA;
        }

        // object  ->  jsonString
        if (typeof valA === 'object') {
            valA = JSON.stringify(valA);
            valB = JSON.stringify(valB);
        }

        // string
        const comparison = String(valA).localeCompare(String(valB));
        return sortState.order === 'asc' ? comparison : -comparison;
    });
}

/**
 * 表格排序（通过 指定列索引 和 排序顺序）
 *
 * @param rows     表格行元素数组
 * @param colIndex 排序列 索引
 * @param order    排序顺序：asc（默认）/ desc
 */
function sortTableByColumn(rows, colIndex, order = 'asc') {

    rows.sort((a, b) => {
        let valA = a.cells[colIndex]?.textContent.trim() ?? '';
        let valB = b.cells[colIndex]?.textContent.trim() ?? '';

        // 数字类型
        if (isNumber(valA) && isNumber(valB)) {
            return order === 'asc' ? valA - valB : valB - valA;
        }
        // 字符串类型
        return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);


        // // 数字类型
        // if (isNumber(valA) && isNumber(valB)) {
        //     return order === 'asc' ? valA - valB : valB - valA;
        // }
        // // 字符串类型
        // if (typeof valA === 'string' && typeof valB === 'string') {
        //     return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
        // }
        // // 处理类型不一致或为 null/undefined 的情况
        // if (valA == null && valB == null) return 0;
        // else if (valA == null) return order === 'asc' ? -1 : 1;
        // else if (valB == null) return order === 'asc' ? 1 : -1;
        // else return order === 'asc' ? String(valA).localeCompare(String(valB)) : String(valB).localeCompare(String(valA));
    });

    return rows;
}