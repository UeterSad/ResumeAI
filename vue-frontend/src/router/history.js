import axios from '@/utils/axios-config.js';

export const fetchHistoryPage = async (userId, pageNum = 1, pageSize = 10) => {
    const response = await axios.get('history/getHistory', {
        params: {
            userId,
            pageNum,
            pageSize
        }
    });
    return response.data;
};

// 兼容旧调用名称。
export const getHistoryList = fetchHistoryPage;
