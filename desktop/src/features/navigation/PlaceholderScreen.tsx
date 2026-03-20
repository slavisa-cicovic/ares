type PlaceholderScreenProps = {
  title: string;
  description: string;
};

export function PlaceholderScreen({ title, description }: PlaceholderScreenProps) {
  return (
    <div className="feature-placeholder-page">
      <div className="screen-intro">
        <span className="eyebrow">Desktop Module</span>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>

      <div className="feature-placeholder-card">
        <span className="eyebrow">Next Step</span>
        <strong>{title} desktop workflow is not implemented yet.</strong>
        <p>This area now has a stable place in navigation and can be filled in with real screens next.</p>
      </div>
    </div>
  );
}
