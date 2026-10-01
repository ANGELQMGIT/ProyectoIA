const API_BASE_URL = 'http://localhost:8080/api/v1';

export const apiService = {
  /**
   * Fetch the catalog of available base queries.
   */
  async getReportes() {
    try {
      const response = await fetch(`${API_BASE_URL}/reportes`);
      if (!response.ok) {
        throw new Error(`Error fetching reportes: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error("Failed to fetch reportes:", error);
      throw error;
    }
  },

  /**
   * Execute a text-to-sql report request.
   * @param {Object} request - The request object containing queryBase and prompt
   */
  async executeReporte(request) {
    try {
      const response = await fetch(`${API_BASE_URL}/reportes/ejecutar`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });
      
      const data = await response.json();
      
      if (!response.ok) {
        throw data; // Throw the error object returned by the backend
      }
      
      return data;
    } catch (error) {
      console.error("Failed to execute reporte:", error);
      throw error;
    }
  }
};
