package edu.univ.erp.util;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class TranscriptExporter {

    @SuppressWarnings("unchecked")
    public static File exportCsvForStudent(int studentId, File outFile) throws Exception {
        // candidate classes and method names to try (order matters)
        String[] candidateClasses = {
            "edu.univ.erp.service.GradeService",
            "edu.univ.erp.service.StudentService"
        };
        String[] candidateMethods = {
            "listGradesForStudent",
            "listGrades",
            "getGradesForStudent",
            "getGrades"
        };
        Class<?>[] candidateParamTypes = { int.class, Integer.class };

        Object rawResult = null;
        String tried = "";

        // Try reflection across candidate classes / methods / param types
        for (String clsName : candidateClasses) {
            try {
                Class<?> cls = Class.forName(clsName);
                for (String mName : candidateMethods) {
                    for (Class<?> pType : candidateParamTypes) {
                        tried += String.format("%s.%s(%s)  ", clsName, mName, pType.getSimpleName());
                        try {
                            Method method = cls.getMethod(mName, pType);

                            // First try invoking as static (invoke with null)
                            try {
                                rawResult = method.invoke(null, (pType == Integer.class) ? Integer.valueOf(studentId) : studentId);
                            } catch (Throwable staticInvokeEx) {
                                // If static invocation failed (could be because it's not static),
                                // try to create an instance and invoke as instance method.
                                try {
                                    Object instance = null;
                                    try {
                                        instance = cls.getDeclaredConstructor().newInstance();
                                    } catch (NoSuchMethodException cnf) {
                                        // no default ctor - skip instance invocation
                                    }
                                    if (instance != null) {
                                        rawResult = method.invoke(instance, (pType == Integer.class) ? Integer.valueOf(studentId) : studentId);
                                    }
                                } catch (Throwable instanceInvokeEx) {
                                    // invocation failed on instance too - swallow and continue searching
                                }
                            }

                            if (rawResult != null) break;
                        } catch (NoSuchMethodException ns) {
                            // method not present with this signature; continue
                        } catch (Throwable invokeEx) {
                            // some other reflection issue; continue searching other candidates
                        }
                    }
                    if (rawResult != null) break;
                }
                if (rawResult != null) break;
            } catch (ClassNotFoundException cnf) {
                // class not present - try next class
            }
        }

        if (rawResult == null) {
            throw new Exception("Could not locate a suitable grades provider method. Tried: " + tried
                    + ".\nPlease verify the service class and method name/signature (static/non-static, parameter type int/Integer).");
        }

        // Validate runtime type and convert to List<Map<String,Object>>
        List<Map<String, Object>> rows;
        if (rawResult instanceof List) {
            rows = (List<Map<String, Object>>) rawResult;
        } else {
            throw new Exception("Grades provider returned unexpected type: " + (rawResult == null ? "null" : rawResult.getClass().getName()));
        }

        // Write CSV using only values from rows (blank when null/missing)
        try (FileWriter fw = new FileWriter(outFile)) {
            fw.write("CourseCode,CourseTitle,Section,Credits,Component,Score,MaxScore,Weight,Percent,Grade,FinalGrade\n");

            // Update data writing loop
            for (Map<String, Object> row : rows) {
                String courseCode = CsvUtil.safe(row.get("course_code"));
                String title = CsvUtil.safe(row.get("course_title"));
                String section = CsvUtil.safe(row.get("section_label"));
                String credits = CsvUtil.safe(row.get("credits"));
                String component = CsvUtil.safe(row.get("component"));
                String score = CsvUtil.safe(row.get("score"));
                String maxScore = CsvUtil.safe(row.get("max_score"));
                String weight = CsvUtil.safe(row.get("weight_pct"));
                String finalGrade = CsvUtil.safe(row.get("final_grade"));

                // Calculate percentage and grade
                double scoreVal = parseDouble(score);
                double maxScoreVal = parseDouble(maxScore);
                double weightVal = parseDouble(weight);

                double percentage = maxScoreVal > 0 ? (scoreVal / maxScoreVal) * weightVal : 0.0;
                String percentStr = String.format("%.2f", percentage);

                double gradePercentage = maxScoreVal > 0 ? (scoreVal / maxScoreVal) * 100 : 0.0;
                String grade = calculateGrade(gradePercentage);

                fw.write(CsvUtil.escapeCsv(courseCode) + "," +
                        CsvUtil.escapeCsv(title) + "," +
                        CsvUtil.escapeCsv(section) + "," +
                        CsvUtil.escapeCsv(credits) + "," +
                        CsvUtil.escapeCsv(component) + "," +
                        CsvUtil.escapeCsv(score) + "," +
                        CsvUtil.escapeCsv(maxScore) + "," +
                        CsvUtil.escapeCsv(weight) + "," +
                        CsvUtil.escapeCsv(percentStr) + "," +
                        CsvUtil.escapeCsv(grade) + "," +
                        CsvUtil.escapeCsv(finalGrade) + "\n");
            }
        }

        return outFile;
    }

    // Helper methods moved to class level (previously they were accidentally inside the method)
    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String calculateGrade(double percentage) {
        if (percentage >= 95) return "A+";
        if (percentage >= 90) return "A";
        if (percentage >= 85) return "A-";
        if (percentage >= 80) return "B+";
        if (percentage >= 75) return "B";
        if (percentage >= 70) return "B-";
        if (percentage >= 65) return "C+";
        if (percentage >= 60) return "C";
        if (percentage >= 55) return "C-";
        if (percentage >= 50) return "D";
        return "F";
    }
}
