import { useMemo, useState } from "react";

function ImportSummaryModal({ isOpen, summary, onClose }) {
  if (!isOpen || !summary) return null;

  const {
    imported = 0,
    total = 0,
    message = "",
    results = [],
    warnings = [],
    errors = [],
  } = summary;

  const [detailResult, setDetailResult] = useState(null);

  const fallbackStudents = Array.isArray(summary.students)
    ? summary.students.map((student) => ({
        success: true,
        fullName:
          student.first_name && student.last_name
            ? `${student.first_name} ${student.last_name}`
            : student.firstName && student.lastName
            ? `${student.firstName} ${student.lastName}`
            : student.displayName || student.name || "—",
        email: student.email || student.username || "—",
        faceSummary: null,
        faceImagesUploaded: student.faceImages?.length ?? 0,
        faceImagesAccepted: student.faceImages?.length ?? 0,
        faceImagesRejected: 0,
        errorMessage: null,
      }))
    : [];

  const resultRows = useMemo(() => {
    const baseRows =
      Array.isArray(results) && results.length > 0 ? results : fallbackStudents;

    return baseRows.map((result) => {
      const faceSummary = result.faceSummary || {};
      const faceResults = Array.isArray(faceSummary.results)
        ? faceSummary.results
        : [];

      const acceptedFromResults = faceResults.filter(
        (item) => item.accepted
      ).length;
      const rejectedFromResults = faceResults.filter(
        (item) => !item.accepted
      ).length;

      const accepted =
        result.faceImagesAccepted ??
        acceptedFromResults ??
        faceSummary.acceptedCount ??
        0;

      const rejected =
        result.faceImagesRejected ??
        rejectedFromResults ??
        faceSummary.rejectedCount ??
        0;

      const totalUploaded =
        result.faceImagesUploaded ??
        faceResults.length ??
        faceSummary.totalUploaded ??
        accepted + rejected;

      const failureDetails =
        faceResults.filter((item) => !item.accepted) ?? [];

      const detailEntries = [];

      if (result.errorMessage) {
        detailEntries.push({
          title: "Error",
          message: result.errorMessage,
        });
      }

      failureDetails.forEach((entry) => {
        const label = `Image ${entry.index + 1}`;
        detailEntries.push({
          title: label,
          message: entry.message || "Rejected",
        });
      });

      return {
        ...result,
        accepted,
        rejected,
        totalUploaded,
        details: detailEntries,
      };
    });
  }, [results, fallbackStudents]);

  const handleOverlayClick = () => {
    onClose?.();
    setDetailResult(null);
  };

  const handleContentClick = (event) => {
    event.stopPropagation();
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content import-summary-modal" onClick={handleContentClick}>
        <div className="manage-modal-header">
          <div className="manage-modal-title">
            <h2>Import Summary</h2>
            <div className="manage-summary">
              <div className="name">
                {imported} of {total} students imported
              </div>
              {message && <div className="export-summary-note">{message}</div>}
            </div>
          </div>
          <button onClick={handleOverlayClick} className="close-button">
            ✕
          </button>
        </div>

        <div className="manage-modal-body">
          <div className="import-summary-table-container">
            <table className="data-table small dense">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Images Imported</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {resultRows.length === 0 && (
                  <tr>
                    <td colSpan={6} className="muted-text" style={{ textAlign: "center" }}>
                      No detailed import information available.
                    </td>
                  </tr>
                )}
                {resultRows.map((result, index) => {
                  return (
                    <tr
                      key={`${result.email}-${index}`}
                      className={`import-row ${
                        result.success ? "success" : "error"
                      }`}
                    >
                      <td>{result.fullName || "—"}</td>
                      <td>{result.email || "—"}</td>
                      <td>
                        <span
                          className={`status-pill ${
                            result.success ? "status-success" : "status-error"
                          }`}
                        >
                          {result.success ? "Imported" : "Failed"}
                        </span>
                      </td>
                      <td>
                        <span className="import-count">
                          {result.accepted}
                          {result.totalUploaded > 0 && (
                            <>
                              <span className="count-separator">/</span>
                              <span className="count-total">
                                {result.totalUploaded}
                              </span>
                            </>
                          )}
                        </span>
                      </td>
                      <td>
                        {result.details.length > 0 ? (
                          <button
                            type="button"
                            className="import-detail-button"
                            onClick={() => setDetailResult(result)}
                          >
                            View
                          </button>
                        ) : (
                          <span className="muted-text">
                            {result.success
                              ? "No issues"
                              : "No details"}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {(warnings.length > 0 || errors.length > 0) && (
            <div className="import-summary-messages">
              {warnings.length > 0 && (
                <div className="import-summary-warning">
                  <h4>Warnings</h4>
                  <ul>
                    {warnings.map((warning, warningIndex) => (
                      <li key={warningIndex}>{warning}</li>
                    ))}
                  </ul>
                </div>
              )}
              {errors.length > 0 && (
                <div className="import-summary-error">
                  <h4>Errors</h4>
                  <ul>
                    {errors.map((error, errorIndex) => (
                      <li key={errorIndex}>{error}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="modal-actions">
          <button
            type="button"
            onClick={handleOverlayClick}
            className="btn btn-primary modal-primary"
          >
            Done
          </button>
        </div>
      </div>

      {detailResult && (
        <div
          className="modal-overlay"
          onClick={() => setDetailResult(null)}
        >
          <div
            className="modal-content import-detail-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="manage-modal-header">
              <div className="manage-modal-title">
                <h2>Import Details</h2>
                <div className="manage-summary">
                  <div className="name">{detailResult.fullName}</div>
                  <div className="export-summary-note">
                    {detailResult.email}
                  </div>
                </div>
              </div>
              <button
                onClick={() => setDetailResult(null)}
                className="close-button"
              >
                ✕
              </button>
            </div>

            <div className="manage-modal-body import-detail-body">
              {detailResult.details.length === 0 ? (
                <div className="muted-text">No additional notes.</div>
              ) : (
                <ul className="detail-list">
                  {detailResult.details.map((item, index) => (
                    <li key={index}>
                      <span className="detail-title">{item.title}</span>
                      <span className="detail-message">{item.message}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div className="modal-actions">
              <button
                type="button"
                onClick={() => setDetailResult(null)}
                className="btn btn-primary modal-primary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ImportSummaryModal;

