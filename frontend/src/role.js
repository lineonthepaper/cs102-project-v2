import { useAuth } from "./AuthContext";

export function useRole() {
  const { user } = useAuth();

  if (!user) return "";
  if (user.user.isInstructor) return "instructor";
  if (user.user.isTA) return "teaching assistant";
  if (user.user.isStudent) return "student";

  return "admin";
}
