package cn.bugstack.recite.infrastructure.adapter.persistence;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis 类型处理器：float[] ↔ pgvector VECTOR 类型.
 */
public class PvVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pg = new PGobject();
        pg.setType("vector");
        pg.setValue(vectorToPgString(parameter));
        ps.setObject(i, pg);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    // ---- 格式转换 ----

    /** float[] → "[0.1,0.2,...]" */
    static String vectorToPgString(float[] vec) {
        if (vec == null || vec.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        return sb.append("]").toString();
    }

    /** "[0.1,0.2,...]" → float[] */
    static float[] parseVector(String str) {
        if (str == null || str.isBlank() || "[]".equals(str.trim())) return new float[0];
        String inner = str.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        String[] parts = inner.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i].trim());
        }
        return vec;
    }
}
